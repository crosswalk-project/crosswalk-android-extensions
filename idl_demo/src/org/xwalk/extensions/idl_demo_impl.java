// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class idl_demo_impl {
    private static final String TAG = "idl_demo_impl";
    private final idl_demo mBase;

    public idl_demo_impl(idl_demo instance) {
        mBase = instance;
    }

    public JSONObject onDummy(JSONObject input) {
        return null;
    }

    public JSONObject onHi(JSONObject input) {
        testEventHandler();
        try {
            JSONObject o = new JSONObject();
            o.put("data", "Hi from Java - you said: " + input.getString("words"));
            return o;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    private static final String CMD_DEMOEVENT = "demoevent";
    private void testEventHandler() {
        try {
            JSONObject o = new JSONObject();
            o.put("cmd", CMD_DEMOEVENT);
            o.put("data", "Demo event triggered");
            mBase.broadcastMessage(o.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }
}
