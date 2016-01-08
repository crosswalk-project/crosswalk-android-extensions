// Copyright (c) 2015 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.util.Log;

import com.xiaomi.gamecenter.sdk.entry.MiAccountInfo;
import com.xiaomi.gamecenter.sdk.entry.MiAppInfo;
import com.xiaomi.gamecenter.sdk.entry.MiBuyInfo;
import com.xiaomi.gamecenter.sdk.GameInfoField;
import com.xiaomi.gamecenter.sdk.MiCommplatform;
import com.xiaomi.gamecenter.sdk.MiErrorCode;
import com.xiaomi.gamecenter.sdk.OnExitListner;
import com.xiaomi.gamecenter.sdk.OnLoginProcessListener;
import com.xiaomi.gamecenter.sdk.OnPayProcessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppPurchaseXiaoMiHelper extends InAppPurchaseHelper {
    public InAppPurchaseXiaoMiHelper(Activity activity, int instanceId, InAppPurchaseHelperListener listener) {
        super(activity, instanceId, listener);
    }

    @Override
    public void init(JSONObject options) {
        try {
            MiAppInfo appInfo = new MiAppInfo();
            appInfo.setAppId(options.getString("id"));
            appInfo.setAppKey(options.getString("key"));
            MiCommplatform.Init(mActivity, appInfo);

            MiCommplatform.getInstance().miLogin(mActivity,
                      new OnLoginProcessListener() {
                          @Override
                          public void finishLoginProcess(int result, MiAccountInfo info) {
                              switch(result) {
                                  case MiErrorCode.MI_XIAOMI_PAYMENT_SUCCESS:
                                      mInitialized = true;
                                      mListener.onRequestFinished(mInstanceId, null);
                                      break;
                                  case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_LOGIN_FAIL:
                                  case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_CANCEL:
                                  case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
                                      mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                                      break;
                                  default:
                                      break;
                              }
                          }
                      });
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void purchase(JSONObject order) {
        MiBuyInfo miBuyInfo = new MiBuyInfo();
        miBuyInfo.setCpOrderId(UUID.randomUUID().toString());

        try {
            if (order.has("productId")) miBuyInfo.setProductCode(order.getString("productId"));
            if (order.has("userInfo")) miBuyInfo.setCpUserInfo(order.getString("userInfo"));
            if (order.has("price")) miBuyInfo.setAmount(order.getInt("price"));
            if (order.has("count")) miBuyInfo.setCount(order.getInt("count"));

            if (order.has("extraInfo")) {
                JSONObject extraInfo = order.getJSONObject("extraInfo");
                Bundle bundle = new Bundle();
                bundle.putString(GameInfoField.GAME_USER_BALANCE, extraInfo.getString(GameInfoField.GAME_USER_BALANCE));
                bundle.putString(GameInfoField.GAME_USER_GAMER_VIP, extraInfo.getString(GameInfoField.GAME_USER_GAMER_VIP) );
                bundle.putString(GameInfoField.GAME_USER_LV, extraInfo.getString(GameInfoField.GAME_USER_LV));
                bundle.putString(GameInfoField.GAME_USER_PARTY_NAME, extraInfo.getString(GameInfoField.GAME_USER_PARTY_NAME));
                bundle.putString(GameInfoField.GAME_USER_ROLE_NAME, extraInfo.getString(GameInfoField.GAME_USER_ROLE_NAME));
                bundle.putString(GameInfoField.GAME_USER_ROLEID, extraInfo.getString(GameInfoField.GAME_USER_ROLEID));
                bundle.putString(GameInfoField.GAME_USER_SERVER_NAME, extraInfo.getString(GameInfoField.GAME_USER_SERVER_NAME));
                miBuyInfo.setExtraInfo(bundle);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        MiCommplatform.getInstance().miUniPay(mActivity, miBuyInfo,
                new OnPayProcessListener() {
                    @Override
                    public void finishPayProcess(int result) {
                        switch(result) {
                            case MiErrorCode.MI_XIAOMI_PAYMENT_SUCCESS:
                                mListener.onRequestFinished(mInstanceId, null);
                                break;
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_CANCEL:
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_PAY_FAILURE:
                            case MiErrorCode.MI_XIAOMI_PAYMENT_ERROR_ACTION_EXECUTED:
                                mListener.onRequestFailed(mInstanceId, OPERATION_ERROR);
                                break;
                            default:
                                break;
                        }
                    }
                });
    }
}
