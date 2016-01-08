// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.core.extension.XWalkExtensionContextClient;
import org.xwalk.core.extension.XWalkExternalExtension;

public class InAppPurchaseExtension extends XWalkExternalExtension {
    private final XWalkExtensionContextClient mExtensionContext;

    private static final String TAG = "IAP";

    // Commands used by this extension.
    private static final String CMD_INIT = "init";
    private static final String CMD_QUERYPRODUCTSINFO = "queryProductsInfo";
    private static final String CMD_PURCHASE = "purchase";
    private static final String CMD_GETRECEIPT = "getReceipt";
    private static final String CMD_VALIDATERECEIPT = "validateReceipt";
    private static final String CMD_RESTORE = "restore";

    // Name of the backends.
    private static final String BACKEND_GOOGLE = "Google";
    private static final String BACKEND_XIAOMI = "XiaoMi";

    private Map<Integer, Requester> mRequesterMap = new HashMap<Integer, Requester>();

    public InAppPurchaseExtension(String name, String jsApi, XWalkExtensionContextClient context) {
        super(name, jsApi, context);
        mExtensionContext = context;
    }

    @Override
    public void onMessage(int instanceId, String message) {
        try {
            JSONObject object = new JSONObject(message);
            int requestId = object.getInt("requestId");
            String cmd = object.getString("cmd");
            Requester requester = mRequesterMap.get(instanceId);
            InAppPurchaseHelper helper = null;

            if (requester != null) {
                requester.requestId = requestId;
                helper = requester.helper;
            }

            Log.e(TAG, "Receive command: " + cmd);
            if (cmd.equals(CMD_INIT)) {
                String channel = object.getJSONObject("data").getString("channel");
                helper = createHelperByChannel(instanceId, channel);
                if (helper == null) {
                    JSONObject error = new JSONObject();
                    error.put("name", InAppPurchaseHelper.NOT_SUPPORTED_ERROR);
                    JSONObject response = new JSONObject();
                    response.put("requestId", requestId);
                    response.put("error", error);
                    postMessage(instanceId, response.toString());
                    return;
                }
                mRequesterMap.put(instanceId, new Requester(requestId, helper));
                helper.init(object.getJSONObject("data"));
            } else if (cmd.equals(CMD_PURCHASE)) {
                helper.purchase(object.getJSONObject("data"));
            } else if (cmd.equals(CMD_QUERYPRODUCTSINFO)) {
                helper.queryProductsInfo(object.getJSONArray("data"));
            } else if (cmd.equals(CMD_GETRECEIPT)) {
                helper.getReceipt();
            } else if (cmd.equals(CMD_VALIDATERECEIPT)) {
                helper.validateReceipt();
            } else if (cmd.equals(CMD_RESTORE)) {
                helper.restore();
            } else {
                Log.e(TAG, "Unexpected command received: " + cmd);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Requester requester : mRequesterMap.values()) {
            requester.helper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        for (Requester requester : mRequesterMap.values()) {
            requester.helper.destroy();
        }
        mRequesterMap.clear();
    }

    private class Requester {
        public Requester(Integer requestId, InAppPurchaseHelper helper) {
            this.requestId = requestId;
            this.helper = helper;
        }

        public Integer requestId;
        public InAppPurchaseHelper helper;
    }

    private InAppPurchaseHelper createHelperByChannel(int instanceId, String channel) {
        if (channel.equals(BACKEND_GOOGLE)) {
            return new InAppPurchaseGoogleHelper(mExtensionContext.getActivity(), instanceId, mHelperListener);
        } else if (channel.equals(BACKEND_XIAOMI)) {
            // TODO(minggang): add the support of XiaoMi, when it's ready.
        }

        Log.e(TAG, "Unsupported channel: " + channel);
        return null;
    }

    private InAppPurchaseHelper.InAppPurchaseHelperListener mHelperListener =
            new InAppPurchaseHelper.InAppPurchaseHelperListener() {
                public void onRequestFinished(int instanceId, Object response) {
                    try {
                        JSONObject object = new JSONObject();
                        object.put("requestId", mRequesterMap.get(instanceId).requestId);
                        object.put("data", response);
                        postMessage(instanceId, object.toString());
                    } catch (JSONException e) {
                        Log.e(TAG, e.toString());
                    }
                }

                public void onRequestFailed(int instanceId, String errorName) {
                    try {
                        JSONObject error = new JSONObject();
                        error.put("name", errorName);
                        JSONObject object = new JSONObject();
                        object.put("requestId", mRequesterMap.get(instanceId).requestId);
                        object.put("error", error);
                        postMessage(instanceId, object.toString());
                    } catch (JSONException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };
}
