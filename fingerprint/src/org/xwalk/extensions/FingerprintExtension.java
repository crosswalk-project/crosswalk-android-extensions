// Copyright (c) 2016 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.core.extension.XWalkExtensionContextClient;
import org.xwalk.core.extension.XWalkExternalExtension;

public class FingerprintExtension extends XWalkExternalExtension {
    private final FingerprintManager mFingerprintManager;
    private CancellationSignal mCancellationSignal;
    private boolean mAuthenticating = false;
    private int mRequestId;
    private int mInstanceId;

    // The names of errors.
    public static final String OPERATION_ERROR = "OperationError";
    public static final String NOT_SUPPORT_ERROR = "NotSupportError";

    // Command used by this extension.
    private static final String CMD_AUTHENTICATE = "authenticate";

    private static final String TAG = "Fingerprint";

    public FingerprintExtension(String extensionName, String jsApi, XWalkExtensionContextClient context) {
        super(extensionName, jsApi, context);
        mFingerprintManager = context.getContext().getSystemService(FingerprintManager.class);
    }

    public boolean isFingerprintAvailable() {
        return mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
    }

    @Override
    public void onMessage(int instanceId, String message) {
        try {
            JSONObject object = new JSONObject(message);

            String cmd = object.getString("cmd");
            int requestId = object.getInt("requestId");

            Log.e(TAG, "Receive command: " + cmd);
            if (cmd.equals(CMD_AUTHENTICATE)) {
                if (!isFingerprintAvailable()) {
                    postErrorMessage(instanceId, requestId, NOT_SUPPORT_ERROR);
                    return;
                }

                if (mAuthenticating) {
                    Log.e(TAG, "Error: Authentication is already running.");
                    postErrorMessage(instanceId, requestId, OPERATION_ERROR);
                    return;
                }

                mInstanceId = instanceId;
                mRequestId = requestId;
                mAuthenticating = true;
                mCancellationSignal = new CancellationSignal();
                mFingerprintManager.authenticate(null, mCancellationSignal, 0, mAuthenticationCallback, null);
            } else {
                Log.e(TAG, "Unexpected command received: " + cmd);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    protected void postErrorMessage(int instanceId, int requestId, String errorName) {
        try {
            JSONObject error = new JSONObject();
            error.put("name", errorName);
            JSONObject object = new JSONObject();
            object.put("requestId", requestId);
            object.put("error", error);
            postMessage(instanceId, object.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    protected void postSuccessMessage(int instanceId, int requestId) {
        try {
            JSONObject object = new JSONObject();
            object.put("requestId", mRequestId);
            postMessage(mInstanceId, object.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    protected void stopAuthenticating() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
        mAuthenticating = false;
    }

    private FingerprintManager.AuthenticationCallback mAuthenticationCallback =
            new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    postErrorMessage(mInstanceId, mRequestId, OPERATION_ERROR);
                    stopAuthenticating();
                    Log.e(TAG, "Error: errorCode = " + errorCode);
                }

                @Override
                public void onAuthenticationFailed() {
                    postErrorMessage(mInstanceId, mRequestId, OPERATION_ERROR);
                    stopAuthenticating();
                    Log.e(TAG, "Error: Authentication Failed.");
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    postSuccessMessage(mInstanceId, mRequestId);
                    mAuthenticating = false;
                    Log.e(TAG, "Authentication Succeeded.");
                }

                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    postErrorMessage(mInstanceId, mRequestId, OPERATION_ERROR);
                    stopAuthenticating();
                    Log.e(TAG, "Error: errorCode = " + helpCode);
                }
            };
}
