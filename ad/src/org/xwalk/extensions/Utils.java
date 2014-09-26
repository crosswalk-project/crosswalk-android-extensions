// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ad;

import org.json.JSONObject;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;

class Utils {
    public static void postErrorMessage(XWalkExtensionClient extensionClient,
                                        int instanceId,
                                        String asyncCallId,
                                        String errorMsg) {
        JSONObject jsonOutput = null;
        try {
            jsonOutput = new JSONObject();
            jsonOutput.put("asyncCallId", asyncCallId);
            jsonOutput.put("error", true);
            JSONObject jsData = new JSONObject();
            jsonOutput.put("data", jsData);
            jsData.put("msg", errorMsg);
        } catch (Exception e) {
             e.printStackTrace();
             return;
        }

        extensionClient.postMessage(instanceId, jsonOutput.toString());
    }

    public static void postSuccessMessage(XWalkExtensionClient extensionClient,
                                          int instanceId,
                                          String asyncCallId) {
        JSONObject jsonOutput = null;
        try {
            jsonOutput = new JSONObject();
            jsonOutput.put("asyncCallId", asyncCallId);
            jsonOutput.put("error", false);
            JSONObject jsData = new JSONObject();
            jsonOutput.put("data", jsData);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        extensionClient.postMessage(instanceId, jsonOutput.toString());
    }
}
