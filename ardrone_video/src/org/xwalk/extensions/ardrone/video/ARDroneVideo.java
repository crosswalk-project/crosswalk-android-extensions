// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ardrone.video;

import android.util.Log;
import android.content.Context;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

public class ARDroneVideo extends XWalkExtensionClient {
    private static final String TAG = "ARDroneVideoExtension";

    private ARDroneVideoOption mOption;
    private InputStream mVideoStream;
    private Thread mParse2RawH264Thread;
    private DecodeRunnable mRunnable;

    private Context mContext;

    public ARDroneVideo(String name, String jsApiContent, XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
        mContext = xwalkContext.getContext();
    }

    @Override
    public void onResume() {
        if (mRunnable != null) mRunnable.onResume();
    }

    @Override
    public void onPause() {
        if (mRunnable != null) mRunnable.onPause();
    }

    @Override
    public void onStop() {
        cleanUp();
    }
    @Override
    public void onDestroy() {
        cleanUp();
    }

    @Override
    public void onMessage(int instanceID, String message) {
        if (message.isEmpty()) return;
        Log.i(TAG, "Receive message: " + message);

        try {
            JSONObject jsonInput = new JSONObject(message);
            String cmd = jsonInput.getString("cmd");

            JSONObject jsonOutput = new JSONObject();
            if (cmd.equals("play")) {
                jsonOutput.put("data", handlePlay(jsonInput.getJSONObject("option")));
            } else if (cmd.equals("stop")) {
                jsonOutput.put("data", handleStop());
            } else if (cmd.equals("removeFile")) {
                jsonOutput.put("data", handleRemoveFile(jsonInput.getString("path")));
            } else {
                jsonOutput.put("data", setErrorMessage("Unsupportted cmd " + cmd));
            }

            jsonOutput.put("asyncCallId", jsonInput.getString("asyncCallId"));
            this.postMessage(instanceID, jsonOutput.toString());
        } catch (JSONException e) {
            printErrorMessage(e);
        }
    }

    @Override
    public String onSyncMessage(int instanceID, String message) {
        return null;
    }

    private void printErrorMessage(JSONException e) {
        Log.e(TAG, e.toString());
    }

    private JSONObject setErrorMessage(String error) {
        JSONObject out = new JSONObject();
        JSONObject errorMessage = new JSONObject();
        try {
            errorMessage.put("message", error);
            out.put("error", errorMessage);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return out;
    }

    private JSONObject handlePlay(JSONObject option) {
        mOption = new ARDroneVideoOption(option);
        if (mOption.codec() == ARDroneVideoCodec.UNKNOWN || mOption.channel() == ARDroneVideoChannel.UNKNOWN)
            return setErrorMessage("Wrong options passed in.");

        try {
            InetAddress address = null;
            // TODO(halton): use -java7 to support multiple catch
            //  catch (IOException | UnknownHostException e) {
            try {
                address = InetAddress.getByName(mOption.ipAddress());
            } catch (UnknownHostException e) {
                Log.e(TAG, e.toString());
            }

            Socket socket = new Socket(address, mOption.port());
            mVideoStream = socket.getInputStream();
            mRunnable = new DecodeRunnable();
            mParse2RawH264Thread = new Thread(mRunnable);
            mParse2RawH264Thread.start();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        return new JSONObject();
    }

    private JSONObject handleStop() {
        cleanUp();
        return new JSONObject();
    }

    private JSONObject handleRemoveFile(String path) {
        File f = new File(path);

        if (!f.isFile()) return setErrorMessage("Invalid path: " + path);
        if (!f.delete()) return setErrorMessage("Failed to delete path: " + path);

        Log.i(TAG, "Successfully remove file: " + path);
        return new JSONObject();
    }

    private void cleanUp() {
        if (mRunnable != null) mRunnable.cleanUp();

        if (mParse2RawH264Thread != null) {
            mParse2RawH264Thread.interrupt();
            mParse2RawH264Thread = null;
        }
    }

    private class DecodeRunnable implements Runnable {
        private Object mPauseLock;
        private boolean mPaused;
        private boolean mFinished;

        private int mVideoCounter;
        private Date startTime;

        private File mVideoCachedDir;

        public DecodeRunnable() {
            mPauseLock = new Object();
            mPaused = false;
            mFinished = false;

            mVideoCounter = 0;
            startTime = new Date();

            mVideoCachedDir = null;
        }

        @Override
        public void run() {
            P264Decoder p264Decoder = new P264Decoder();

            if (mVideoCachedDir == null) {
                mVideoCachedDir = new File(mContext.getCacheDir() + "/video");
            }

            // Clean and recreated cached dir 
            if (mVideoCachedDir != null) deleteDir(mVideoCachedDir);
            mVideoCachedDir.mkdir();

            while (!mFinished) {
                Date currentTime = new Date();
                ++mVideoCounter;

                File h264File = new File(mVideoCachedDir, mVideoCounter + ".h264");
                File mp4File = new File(mVideoCachedDir, mVideoCounter + ".mp4");
                try {
                    byte[] bytes = p264Decoder.readFrames(mVideoStream, mOption.latency());
                    Log.i(TAG, "Current h264 file is: " + h264File.getAbsolutePath() + "buffer size:" + bytes.length);
                    Log.i(TAG, "Duration of " + mVideoCounter + " is: " + (currentTime.getTime() - startTime.getTime()));
                    startTime = currentTime;

                    if (bytes == null) continue;

                    FileOutputStream h264OutputStream = new FileOutputStream(h264File, false);
                    h264OutputStream.write(bytes);
                    h264OutputStream.flush();
                    h264OutputStream.close();

                    Log.i(TAG, "Current mp4 file is " + mp4File.getAbsolutePath());
                    muxerH264ToMp4(h264File, mp4File);

                    h264File.delete();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }

                if (mp4File.exists()) {
                    JSONObject out = new JSONObject();
                    try {
                        out.put("reply", "newvideoready");
                        JSONObject path = new JSONObject();
                        path.put("absolutePath", mp4File.getAbsolutePath());
                        out.put("data", path);

                        broadcastMessage(out.toString());
                    } catch (JSONException e) {
                        printErrorMessage(e);
                    }
                }

                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                            mPauseLock.wait();
                        } catch (InterruptedException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                }
            }
        }

        public void onPause() {
            synchronized (mPauseLock) {
                mPaused = true;
            }
        }

        public void onResume() {
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }

        public void cleanUp() {
            if (mVideoCachedDir != null) deleteDir(mVideoCachedDir);
        }

        private void muxerH264ToMp4(File h264File, File mp4File) {
            try {
                H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(h264File.getAbsolutePath()));
                Movie m = new Movie();
                m.addTrack(h264Track);

                Container out = new DefaultMp4Builder().build(m);

                FileOutputStream fos = new FileOutputStream(mp4File);
                FileChannel fc = fos.getChannel();
                out.writeContainer(fc);
                fos.close();

            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        private boolean deleteDir(File dir) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        }

    }
}
