// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

import org.xwalk.extensions.util.IabHelper;
import org.xwalk.extensions.util.IabResult;
import org.xwalk.extensions.util.Inventory;
import org.xwalk.extensions.util.Purchase;
import org.xwalk.extensions.util.SkuDetails;

public class iap extends XWalkExtensionClient {
    final private XWalkExtensionContextClient mExtensionContext;

    private static final String TAG = "IAP";
    private static final String CMD_INIT = "init";
    private static final String CMD_QUERYPRODUCTS = "query_products";
    private static final String CMD_BUY = "buy";

    // Request code for the purchase flow
    private static final int RC_REQUEST = 10001;

    private boolean mInitialized;
    private IabHelper mHelper;

    // For querying by async methods: cmd => <InstanceID, jsonOutput>
    private Map<String, Pair<Integer, JSONObject>> cmdMap =
        new HashMap<String, Pair<Integer, JSONObject>>();

    public iap(String name, String JsApiContent, XWalkExtensionContextClient context) {
        super(name, JsApiContent, context);
        mExtensionContext = context;
    }

    @Override
    public void onMessage(int instanceId, String message) {
        if (message.isEmpty()) return;
        try {
            JSONObject jsonInput = new JSONObject(message);
            JSONObject jsonOutput = new JSONObject();
            jsonOutput.put("_promise_id", jsonInput.getString("_promise_id"));
            String cmd = jsonInput.getString("cmd");
            if (cmd.equals(CMD_INIT)) {
                init(jsonInput.getString("key"));
                postMessage(instanceId, jsonOutput.toString());
                return;
            }

            cmdMap.put(cmd, new Pair<Integer, JSONObject>(instanceId, jsonOutput));
            if (cmd.equals(CMD_BUY)) {
                purchase(jsonInput.getString("id")); // will consume it immediately after purchase finished.
            } else if (cmd.equals(CMD_QUERYPRODUCTS)) {
                queryProductDetails(jsonInput.getString("ids"));
            } else {
                Log.e(TAG, "Unexpected message received: " + message);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null ||
            mHelper.handleActivityResult(requestCode, resultCode, data)) { // Handled
            return;
        }

        // Sometimes it is not handled, e.g. user directly close the dialog
        // In this case need to call super's method, so that the callback function can be involved
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void init(String base64EncodedPublicKey) {
        if (mInitialized) return;

        final boolean isDebug =
            (0 != (mExtensionContext.getActivity().getApplicationInfo().flags &
                   ApplicationInfo.FLAG_DEBUGGABLE));

        mHelper = new IabHelper(mExtensionContext.getActivity(), base64EncodedPublicKey);
        mHelper.enableDebugLogging(isDebug);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.e(TAG, "Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                mInitialized = true;
            }
        });
    }

    // Callback for when a query for product details is finished
    private IabHelper.QueryInventoryFinishedListener mGotDetailsListener =
        new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (!ensureResult(result)) return;

                try {
                    List<SkuDetails> skuList = inventory.getAllProducts();
                    JSONArray jsonSkuList = new JSONArray();
                    for (SkuDetails sku : skuList) {
                        jsonSkuList.put(sku.toJson());
                    }
                    Pair<Integer, JSONObject> output = cmdMap.get(CMD_QUERYPRODUCTS);
                    output.second.put("data", jsonSkuList);
                    postMessage(output.first, output.second.toString());
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }
            }
        };

    // Callback for when a purchase is finished
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
        new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                if (!ensureResult(result)) return;
                consume(purchase);
            }
        };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
        new IabHelper.OnConsumeFinishedListener() {
            public void onConsumeFinished(Purchase purchase, IabResult result) {
                if (!result.isSuccess()) {
                    Log.e(TAG, "Error while consuming: " + result);
                    return;
                }

                Pair<Integer, JSONObject> output = cmdMap.get(CMD_BUY);
                if (output == null || output.second == null) return;
                try {
                    JSONObject o = new JSONObject();
                    o.put("orderId", purchase.getOrderId());
                    o.put("packageName", purchase.getPackageName());
                    o.put("productId", purchase.getSku());
                    o.put("purchaseTime", purchase.getPurchaseTime());
                    int state = purchase.getPurchaseState();
                    if (state == 0) {
                        o.put("purchaseState", "purchased");
                    } else if (state == 1) {
                        o.put("purchaseState", "canceled");
                    } else if (state == 2) {
                        o.put("purchaseState", "refunded");
                    }
                    o.put("token", purchase.getToken());

                    output.second.put("data", o);
                    postMessage(output.first, output.second.toString());
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }
            }
        };

    // IN: ids - A JSONString format string, e.g. ["gas", "premium"]
    // OUT: result will be posted back in callback method.
    private void queryProductDetails(String ids) {
        if (!inited() || ids == null) return;

        final List<String> productList = new ArrayList<String>();
        try {
            final JSONArray jsonIds = new JSONArray(ids);
            for (int i = 0; i < jsonIds.length(); ++i) {
                productList.add(jsonIds.get(i).toString());
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return;
        }

        mExtensionContext.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mHelper.flagEndAsync();
                    mHelper.queryInventoryAsync(true, productList, mGotDetailsListener);
                } catch (java.lang.IllegalStateException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private void purchase(String skuId) {
        if (!inited() || skuId == null) return;

        try {
            mHelper.flagEndAsync();
            mHelper.launchPurchaseFlow(mExtensionContext.getActivity(), skuId,
                    RC_REQUEST, mPurchaseFinishedListener, "");
        } catch (java.lang.IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void consume(Purchase purchase) {
        if (!inited() || purchase == null) return;

        // e.g. no need to call consume for IabHelper.ITEM_TYPE_SUBS
        if (purchase.getItemType() != IabHelper.ITEM_TYPE_INAPP) {
            return;
        }

        final Purchase p = purchase;
        mExtensionContext.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mHelper.flagEndAsync();
                    mHelper.consumeAsync(p, mConsumeFinishedListener);
                } catch (java.lang.IllegalStateException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private Boolean inited() {
        if (!mInitialized){
            Log.e(TAG, "Billing plugin was not initialized");
            return false;
        }
        return true;
    }

    // Check if there is any errors in the iabResult
    private Boolean ensureResult(IabResult result) {
        if (result.isFailure()) {
            Log.e(TAG, "IabResult error: " + result);
            return false;
        }

        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
            Log.e(TAG, "The billing helper has been disposed");
            return false;
        }

        return true;
    }
}
