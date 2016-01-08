// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.extensions.util.IabHelper;
import org.xwalk.extensions.util.IabResult;
import org.xwalk.extensions.util.Inventory;
import org.xwalk.extensions.util.Purchase;
import org.xwalk.extensions.util.SkuDetails;

public class InAppPurchaseGoogleHelper extends InAppPurchaseHelper {
    private IabHelper mIab;

    // Request code for the purchase flow.
    private static final int RC_REQUEST = 10001;

    public InAppPurchaseGoogleHelper(Activity activity, int instanceId, InAppPurchaseHelperListener listener) {
        super(activity, instanceId, listener);
    }

    @Override
    public void init(JSONObject options) {
        try {
            mDebugEnabled = options.getBoolean("debug");
            mIab = new IabHelper(mActivity, options.getString("key"));
            mIab.enableDebugLogging(mDebugEnabled);
            mIab.startSetup(
                    new IabHelper.OnIabSetupFinishedListener() {
                        public void onIabSetupFinished(IabResult result) {
                            if (mIab == null) return;

                            if (result.isFailure()) {
                                Log.e(TAG, "Problem setting up in-app billing: " + result);
                                mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                            } else {
                                mInitialized = true;
                                mListener.onRequestFinished(mInstanceId, null);
                            }
                        }
                    });
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void queryProductsInfo(JSONArray productIds) {
        final List<String> productList = new ArrayList<String>();
        for (int i = 0; i < productIds.length(); i++) {
            productList.add(productIds.optString(i));
        }

        mActivity.runOnUiThread(
                new Runnable() {
                    public void run() {
                        try {
                            mIab.flagEndAsync();
                            mIab.queryInventoryAsync(true, productList, mGotDetailsListener);
                        } catch (java.lang.IllegalStateException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
    }

    @Override
    public void purchase(JSONObject order) {
        try {
            mIab.flagEndAsync();
            try {
                mIab.launchPurchaseFlow(mActivity, order.getString("productId"),
                        RC_REQUEST, mPurchaseFinishedListener, "");
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        } catch (java.lang.IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void destroy() {
      if (mIab != null) {
        mIab.dispose();
        mIab = null;
      }
    }

    @Override
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (mIab != null) mIab.handleActivityResult(requestCode, resultCode, data);
    }

    // Callback for when a query for product details is finished.
    private IabHelper.QueryInventoryFinishedListener mGotDetailsListener =
            new IabHelper.QueryInventoryFinishedListener() {
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (mIab == null) return;

                    if (result.isFailure()) {
                        mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                        return;
                    }

                    try {
                        List<SkuDetails> skuList = inventory.getAllProducts();
                        JSONArray jsonSkuList = new JSONArray();
                        for (SkuDetails sku : skuList) {
                            jsonSkuList.put(sku.toJson());
                        }
                        mListener.onRequestFinished(mInstanceId, jsonSkuList);
                    } catch (JSONException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };

    // Callback for when a purchase is finished.
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener() {
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    if (mIab == null) return;

                    if (result.isFailure()) {
                        mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                        return;
                    }

                    if (purchase.getItemType() != IabHelper.ITEM_TYPE_INAPP) {
                        mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                    } else {
                        final Purchase p = purchase;
                        mActivity.runOnUiThread(
                                new Runnable() {
                                    public void run() {
                                        try {
                                            mIab.flagEndAsync();
                                            mIab.consumeAsync(p, mConsumeFinishedListener);
                                        } catch (java.lang.IllegalStateException e) {
                                            Log.e(TAG, e.toString());
                                        }
                                    }
                                });
                    }
                }
            };


    // Called when consumption is completed.
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
            new IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (mIab == null) return;

                    if (result.isFailure()) {
                        mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                        return;
                    }

                    try {
                        if (purchase.getPurchaseState() == 0) {
                            JSONObject purchaseResult = new JSONObject();
                            purchaseResult.put("packageName", purchase.getPackageName());
                            purchaseResult.put("productId", purchase.getSku());
                            purchaseResult.put("purchaseTime", purchase.getPurchaseTime());
                            mListener.onRequestFinished(mInstanceId, purchaseResult);
                        } else {
                            mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };
}
