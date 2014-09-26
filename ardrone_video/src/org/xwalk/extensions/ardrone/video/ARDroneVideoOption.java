// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ARDroneVideoOption {
    private static final String TAG = "ARDroneVideoOption";

    private String mIpAddress;
    private int mPort;
    private long mLatency;
    private long mBitrate;
    private ARDroneVideoCodec mCodec;
    private ARDroneVideoChannel mChannel;

    public ARDroneVideoOption(JSONObject option) {
        try {
            mIpAddress = option.getString("ipAddress");
            mPort = option.getInt("port");
            mLatency = option.getLong("latency");
            mBitrate = option.getLong("bitrate");

            long codec = option.getLong("codec");
            boolean found = false;
            for (ARDroneVideoCodec c : ARDroneVideoCodec.values()) {
                if (codec == c.getValue()) {
                    mCodec = c;
                    found = true;
                    break;
                }
            }
            if (!found) mCodec = ARDroneVideoCodec.UNKNOWN;

            long channel = option.getLong("channel");
            found = false;
            for (ARDroneVideoChannel c : ARDroneVideoChannel.values()) {
                if (channel == c.getValue()) {
                    mChannel = c;
                    found = true;
                    break;
                }
            }
            if (!found) mChannel = ARDroneVideoChannel.UNKNOWN;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    public String ipAddress() { return mIpAddress; }
    public int port() { return mPort; }
    public long latency() { return mLatency; }
    public long bitrate() { return mBitrate; }
    public ARDroneVideoCodec codec() { return mCodec; }
    public ARDroneVideoChannel channel() { return mChannel; }
}
