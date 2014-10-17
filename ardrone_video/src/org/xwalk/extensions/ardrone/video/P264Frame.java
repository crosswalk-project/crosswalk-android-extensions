// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/*
 * The PaVE header defination can be found at
 * https://github.com/elliotwoods/ARDrone-GStreamer-test/blob/master/plugin/src/pave.h
 */
public class P264Frame {
    private static final String TAG = "P264Frame";

    private byte[] mPayload;
    private int mPayloadLength;
    private byte mFrameType;

    private enum P264FrameType {
        UNKNOWN,
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
                case UNKNOWN:
                default:
                    return 0x00;
            }
        }
    };

    public P264Frame() {
        mPayload = null;
        mPayloadLength = 0;
        mFrameType = P264FrameType.UNKNOWN.getValue();
    }

    public boolean isStartFrame() {
        return mFrameType == P264FrameType.IDR_FRAME.getValue();
    }

    public byte[] getPayload() { return mPayload; }

    public boolean getNextH264RawFrame(InputStream inputStream) throws IOException {
        // The PaVE header defination can be found at
        // https://github.com/elliotwoods/ARDrone-GStreamer-test/blob/master/plugin/src/pave.h
        //
        // Reference code from https://github.com/bkw/node-dronestream/blob/master/lib/PaVEParser.js
        byte[] bytes = new byte[4];
        inputStream.read(bytes, 0, 4);
        String signature = new String(bytes, "US-ASCII");
        if (!signature.equals("PaVE")) {
            Log.e(TAG, "Wrong signature: " + signature);
            return false;
        }

        // Skip version(1) and video_codec(1)
        inputStream.skip(2);

        // header_size
        bytes = readDesiredBytes(inputStream, 2);
        int headerSize = unsignedIntBytes2Int(bytes);
        Log.i(TAG, "Header size: " + headerSize);

        // payload size
        bytes = readDesiredBytes(inputStream, 4);
        mPayloadLength = unsignedIntBytes2Int(bytes);
        Log.i(TAG, "Payload size: " + mPayloadLength);

        // Skip 18 bytes:
        // encoded_stream_width 2
        // encoded_stream_height 2
        // display_width 2
        // display_height 2
        // frame_number 4
        // timestamp 4
        // total_chunks 1
        // chunk_index 1
        inputStream.skip(18);

        // frame type
        mFrameType = (byte)inputStream.read();
        Log.i(TAG, "Frame type: " + mFrameType);

        // bytes consumed so far: 4 + 2 + 2 + 4 + 18 + 1 = 31. Skip ahead.
        inputStream.skip(headerSize - 31);

        mPayload = readDesiredBytes(inputStream, mPayloadLength);
        return true;
    }

    private byte[] readDesiredBytes(InputStream is, int size) throws IOException {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[size];

        while ((bytesRead = is.read(data, offset, data.length - offset)) != -1) {
            offset += bytesRead;
            if (offset >= data.length) {
              return data;
            }
        }

        throw new EOFException();
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

    public static final byte[] appendData(byte[] arr1,byte[] arr2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (arr1 != null && arr1.length != 0)
                outputStream.write(arr1);
            if (arr2 != null && arr2.length != 0)
                outputStream.write(arr2);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return outputStream.toByteArray();
    }
}
