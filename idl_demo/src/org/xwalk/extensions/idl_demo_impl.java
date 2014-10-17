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

    // 1. Test event handler
    // 2. Add string to input string and return back to JS
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

    // Return a + b
    public JSONObject onCalculate(JSONObject input) {
        try {
            JSONObject o = new JSONObject();
            long a = input.getLong("a");
            long b = input.getLong("b");
            o.put("sum", a + b);
            return o;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    // Broadcast event to JS
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
