// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class idl_demo_impl {
    private static final String TAG = "idl_demo_impl";

    public JSONObject onDummy(JSONObject input) {
        return null;
    }

    public JSONObject onHi(JSONObject input) {
        try {
            JSONObject o = new JSONObject();
            o.put("data", "Hi from Java - you said: " + input.getString("words"));
            return o;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}
