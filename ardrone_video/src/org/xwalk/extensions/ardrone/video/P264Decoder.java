// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

/*
 * The PaVE header defination can be found at
 * https://github.com/elliotwoods/ARDrone-GStreamer-test/blob/master/plugin/src/pave.h
 */
public class P264Decoder {
    private static final String TAG = "P264Decoder";
    private byte[] mStartFrame;
    private Date mStartTime;

    public P264Decoder () {
        mStartFrame = null;
        mStartTime = new Date();
    }

    // Return buffer start with IDR-Frame, end with previous frame before second IDR-Frame
    public byte[] readFrames(InputStream inputStream, long latency) throws IOException {
        P264Frame frame = new P264Frame();
        byte[] result = null;

        frame.getNextH264RawFrame(inputStream);

        // Check whether it is first-time reading.
        if (mStartFrame == null) {
            if (!frame.isStartFrame()) return null;

            mStartFrame = frame.getPayload().clone();
            result = mStartFrame.clone();
        } else {
            result = appendData(mStartFrame, frame.getPayload().clone());
        }

        // Appending P-Frames until meet next IDR-Frame
        while (true) {
            frame.getNextH264RawFrame(inputStream);
            if (!frame.isStartFrame()) {
                result = appendData(result, frame.getPayload().clone());
                continue;
            }

            mStartFrame = frame.getPayload().clone();

            // Based on testing, two IDR-Frames duration always bigger than 100ms.
            if (latency < 100) {
                break;
            } else {
                Date currentTime = new Date();
                if (currentTime.getTime() - mStartTime.getTime() > latency) {
                    mStartTime = currentTime;
                    break;
                }
            }
        }

        return result;
    }

    protected byte[] appendData(byte[] arr1,byte[] arr2) {
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
