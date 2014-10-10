// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone_pilot;

import android.util.Log;

import java.util.concurrent.PriorityBlockingQueue;

public class ATCommandQueue {
    private static final String TAG = "ATCommandQueue"; 

    private int mMaxSize;
    private PriorityBlockingQueue<ATCommand> mQueue;

    public ATCommandQueue(int maxSize) {
        mMaxSize = maxSize;
        mQueue = new PriorityBlockingQueue<ATCommand>(maxSize);
    }

    public void add(ATCommand atCommand) {
        // FIXME(legend): ignore the commands more than max size.
        if (mQueue.size() < mMaxSize) {
            mQueue.add(atCommand);
        }
    }

    public ATCommand take() {
        ATCommand atCommand = null;
        try {
            atCommand = mQueue.take();
        } catch (InterruptedException e) {
            return null;
        }

        return atCommand;
    }
}
