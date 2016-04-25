// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.app.Activity;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

public class InAppPurchaseHelper {
    protected int mInstanceId;
    protected Activity mActivity;
    protected InAppPurchaseHelperListener mListener;
    protected boolean mInitialized;
    protected boolean mDebugEnabled;

    protected static final String TAG = "IAP";

    // The names of different errors.
    public static final String OPERATION_ERROR = "OperationError";
    public static final String NOT_SUPPORTED_ERROR = "NotSupportedError";

    public InAppPurchaseHelper(Activity activity, int instanceId, InAppPurchaseHelperListener listener) {
        mActivity = activity;
        mInstanceId = instanceId;
        mListener = listener;
    }

    public boolean isInitializd() {
        return mInitialized;
    }

    public interface InAppPurchaseHelperListener {
        /**
         * Callback when the request finished successfully.
         */
        public void onRequestFinished(int instanceId, Object response);

        /**
         * Callback when an error occurs.
         */
        public void onRequestFailed(int instanceId, String errorName);
    }

    /**
     * Called when init command is received.
     */
    public void init(JSONObject options) {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    /**
     * Called when queryProductsInfo command is received.
     */
    public void queryProductsInfo(JSONArray productIds) {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    /**
     * Called when purchase command is received.
     */
    public void purchase(JSONObject order) {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    /**
     * Called when getReceipt command is received.
     */
    public void getReceipt() {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    /**
     * Called when validateReceipt command is received.
     */
    public void validateReceipt() {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    /**
     * Called when restore command is received.
     */
    public void restore() {
        mListener.onRequestFailed(mInstanceId, NOT_SUPPORTED_ERROR);
    }

    public void destroy() {
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
    }
}
