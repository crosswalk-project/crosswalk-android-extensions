// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ad;

import com.google.android.gms.ads.*;

import java.util.HashMap;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

import org.json.JSONObject;

public class Advertising extends XWalkExtensionClient {
    private XWalkExtensionContextClient mExtensionContext = null;
    private HashMap<String, IAdService> mAdServiceMap = null;

    public Advertising(String name, String JsApiContent, XWalkExtensionContextClient context) {
        super(name, JsApiContent, context);
        mExtensionContext = context;
        mAdServiceMap = new HashMap<String, IAdService>();
    }

    @Override
    public void onMessage(int instanceId, String message) {
        JSONObject jsonInput = null;
        String asyncCallId = null;
        String service = null;
        try {
            jsonInput = new JSONObject(message);
            asyncCallId = jsonInput.getString("asyncCallId");
            service = jsonInput.getJSONObject("data").getString("service");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        IAdService adService = mAdServiceMap.get(service);
        if (null == adService) {
            if (service.equals("admob")) {
                adService = new AdAdmob(this, mExtensionContext, instanceId);
                mAdServiceMap.put(service, adService);
            } else {
                Utils.postErrorMessage(this, instanceId, asyncCallId,
                                       "Error: \'" + service + "\' is not supported yet.");
                return;
            }
        }

        adService.handleMessage(message);
    }
}
