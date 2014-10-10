// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone_pilot;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ATCommandManager extends RunnableWithLock {
    public static final String TAG = "ATCommandManager";

    public static final int CMD_PORT = 5556;

    private ATCommandQueue mCommandQueue;
    private DatagramSocket mDataSocket;
    private InetAddress mInetAddress;
    private int mSequence;

    public ATCommandManager(ATCommandQueue queue, String remoteAddress) {
        mCommandQueue = queue;
        mSequence = 1;
        try {
            mDataSocket = new DatagramSocket();
            mInetAddress = InetAddress.getByName(remoteAddress);
        } catch (SocketException e) {
            Log.i(TAG, e.toString());
        } catch (UnknownHostException e) {
            Log.i(TAG, e.toString());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                ATCommand atCommand = mCommandQueue.take();
                byte[] packetData = atCommand.buildPacketBytes(mSequence);
                DatagramPacket datagramPacket = new DatagramPacket(packetData, packetData.length,
                        mInetAddress, CMD_PORT);
                mDataSocket.send(datagramPacket);

                Log.i(TAG, atCommand.buildATCommandString(mSequence));
                mSequence += 1;
                if (atCommand.getCommandType().equals("Quit")) {
                    break;
                }

                Thread.sleep(30);
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                break;
            }
            paused();
        }
    }
}

class RunnableWithLock implements Runnable {
    private boolean mIsPaused;
    private Object mPauseLock;

    public RunnableWithLock() {
        mIsPaused = false;
        mPauseLock = new Object();
    }

    @Override
    public void run() {
    }

    public void onPause() {
        synchronized (mPauseLock) {
            mIsPaused = true;
        }
    }

    public void onResume() {
        synchronized (mPauseLock) {
            mIsPaused = false;
            mPauseLock.notifyAll();
        }
    }

    protected void paused() {
        synchronized (mPauseLock) {
            while (mIsPaused) {
                try {
                    mPauseLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
