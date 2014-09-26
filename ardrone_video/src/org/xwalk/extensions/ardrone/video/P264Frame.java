// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class P264Frame {
    private static final String TAG = "P264Frame";

    private byte[] mHeader;
    private byte[] mPayload;
    private int mPayloadLength;

    private enum P264FrameType {
        UNKNNOWN,
        IDR_FRAME,
        I_FRAME,
        P_FRAME,
        TYPE_HEADERS;

        public byte getValue() {
            switch (this) {
                case IDR_FRAME:
                    return 0x01; /* headers followed by I-frame */    
                case I_FRAME:
                    return 0x02;
                case P_FRAME:
                    return 0x03;
                case TYPE_HEADERS:
                    return 0x04;
                case UNKNNOWN:
                default:
                    return 0x00;
            }
        }
    };

    public P264Frame() {
        mPayload = null;
        mHeader = null;
        mPayloadLength = 0;
    }

    public boolean isStartFrame() {
        return mHeader[30] == P264FrameType.IDR_FRAME.getValue();
    }

    public byte[] getPayload() { return mPayload; }

    public boolean getNextH264RawFrame(InputStream inputStream) throws IOException {
        // The PaVE header defination can be found at
        // https://github.com/elliotwoods/ARDrone-GStreamer-test/blob/master/plugin/src/pave.h
        mHeader = new byte[76];
        inputStream.read(mHeader, 0, 76);

        byte[] bytes = new byte[4];
        System.arraycopy(mHeader, 8, bytes, 0, 4);
        mPayloadLength = unsignedIntBytes2Int(bytes);

        mPayload = new byte[mPayloadLength];

        int index = 0;
        int position = 0;
        while (index < mPayloadLength) {
            position = inputStream.read(mPayload, index, mPayloadLength - index);
            if (position <= 0) {
                break;
            }
            index = index + position;
        }

        return true;
    }

    private int unsignedIntBytes2Int(byte[] bytes) {
        int res = 0;
        if (bytes == null) {
            return res;
        }

        for (int i = 0; i < bytes.length; i++) {
            res = res | ((bytes[i] & 0xff) << i * 8);
        }
        return res;
    }
}
