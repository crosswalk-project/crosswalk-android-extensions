// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.extensions.ad;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.gms.ads.*;

import java.util.HashMap;

import org.json.JSONObject;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;

class AdAdmob implements IAdService {
    private static final HashMap<Integer, String> sAdErrorMsgMap = new HashMap<Integer, String>(){{
                put(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Internal error.");
                put(AdRequest.ERROR_CODE_INVALID_REQUEST, "Invalid request.");
                put(AdRequest.ERROR_CODE_NETWORK_ERROR, "Network error.");
                put(AdRequest.ERROR_CODE_NO_FILL, "No fill.");
            }};
    private static final HashMap<String, AdSize> sAdSizeMap = new HashMap<String, AdSize>() {{
                put("BANNER", AdSize.BANNER);
                put("MEDIUM_RECTANGLE", AdSize.MEDIUM_RECTANGLE);
                put("FULL_BANNER", AdSize.FULL_BANNER);
                put("LEADERBOARD", AdSize.LEADERBOARD);
                put("SMART_BANNER", AdSize.SMART_BANNER);
            }};

    private XWalkExtensionClient mExtensionClient = null;
    private XWalkExtensionContextClient mExtensionContextClient = null;
    private int mInstanceId = 0;
    private int mNextId = 0;
    private HashMap<Integer, AdInstance> mAdInstanceMap = null;

    private AdInstance getInstanceFromMsg(String message) {
        String id = "";
        try {
            JSONObject jsonMsg = new JSONObject(message);
            JSONObject jsonData = jsonMsg.getJSONObject("data");
            id = jsonData.getString("id");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return mAdInstanceMap.get(Integer.parseInt(id));
    }

    private class AdInstance {
        protected static final String sContainerClassName = "XWalkRuntimeView";
        protected XWalkExtensionClient mExtensionClient = null;
        protected XWalkExtensionContextClient mExtensionContextClient = null;
        protected int mInstanceId = 0;
        protected int mId = 0;
        protected String mPublisherId = null;

        public AdInstance(XWalkExtensionClient extensionClient,
                          XWalkExtensionContextClient extensionContextClient,
                          int instanceId,
                          int id) {
            mExtensionClient = extensionClient;
            mExtensionContextClient = extensionContextClient;
            mInstanceId = instanceId;
            mId = id;
        }

        protected void create(String message) {}
        protected void destroy(String message) {}
        protected void show(String message) {}

        protected AdListener createAdListener() {
            return new AdListener() {
                @Override
                public void onAdFailedToLoad(int errorCode) {
                    JSONObject jsonData = null;
                    try {
                        jsonData = new JSONObject();
                        jsonData.put("errorCode", errorCode);
                        jsonData.put("errorMsg", sAdErrorMsgMap.get(errorCode));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    sendEvent("onfailed", jsonData);
                }

                @Override
                public void onAdLoaded() {
                    sendEvent("onloaded", null);
                }

                @Override
                public void onAdOpened() {
                    sendEvent("onopened", null);
                }

                @Override
                public void onAdClosed() {
                    sendEvent("onclosed", null);
                }
            };
        }

        protected void sendEvent(String event, JSONObject body) {
            try {
                JSONObject jsonOutput = new JSONObject();
                jsonOutput.put("cmd", event);
                JSONObject jsData = new JSONObject();
                jsonOutput.put("data", jsData);
                jsData.put("id", mId);
                if (null != body) {
                    jsData.put("body", body);
                }
                mExtensionClient.postMessage(mInstanceId, jsonOutput.toString());
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }
    }

    private class AdViewInstance extends AdInstance {
        private AdView mAdView = null;
        private LinearLayout mXwalkContent = null;
        private AdSize mAdSize = null;
        private Boolean mBannerAtTop = false;
        private Boolean mOverlap = false;

        public AdViewInstance(XWalkExtensionClient extensionClient,
                          XWalkExtensionContextClient extensionContextClient,
                          int instanceId,
                          int id) {
            super(extensionClient, extensionContextClient, instanceId, id);
        }

        @Override
        protected void create(String message) {
            String asyncCallId = ""; 
            try {
                JSONObject jsonMsg = new JSONObject(message);
                JSONObject jsonData = jsonMsg.getJSONObject("data");
                mPublisherId = jsonData.getString("publisherId");
                asyncCallId = jsonMsg.getString("asyncCallId");
                mBannerAtTop = jsonData.getBoolean("bannerAtTop");
                mOverlap = jsonData.getBoolean("overlap");
                String size = jsonData.getString("size");
                mAdSize = sAdSizeMap.get(size);
                if (null == mAdSize) {
                    Utils.postErrorMessage(mExtensionClient, mInstanceId, asyncCallId, "Error: wrong size type.");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    mAdView = new AdView(mExtensionContextClient.getActivity());
                    mAdView.setAdSize(mAdSize);
                    mAdView.setAdUnitId(mPublisherId);
                    mAdView.setAdListener(createAdListener());
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mAdView.loadAd(adRequest);
                }
            };

            mExtensionContextClient.getActivity().runOnUiThread(runnable);

            try {
                JSONObject jsonOutput = new JSONObject();
                jsonOutput.put("asyncCallId", asyncCallId);
                jsonOutput.put("cmd", "create_ret");
                jsonOutput.put("error", false);
                JSONObject jsData = new JSONObject();
                jsonOutput.put("data", jsData);
                JSONObject jsBody = new JSONObject();
                jsData.put("id", mId);
                jsData.put("service", "admob");
                mExtensionClient.postMessage(mInstanceId, jsonOutput.toString());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            return;
        }

        @Override
        protected void destroy(String message) {
            String asyncCallId = "";
            try {
                JSONObject jsonMsg = new JSONObject(message);
                asyncCallId = jsonMsg.getString("asyncCallId");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    FrameLayout rootLayout = (FrameLayout)mExtensionContextClient.getActivity()
                            .getWindow().getDecorView().findViewById(android.R.id.content);
                    removeAdView(rootLayout, mAdView);
                    mAdView.destroy();
                }
            };

            mExtensionContextClient.getActivity().runOnUiThread(runnable);
            Utils.postSuccessMessage(mExtensionClient, mInstanceId, asyncCallId);
        }

        @Override
        protected void show(String message) {
            String asyncCallId = "";
            Boolean shown = false;
            try {
                JSONObject jsonMsg = new JSONObject(message);
                asyncCallId = jsonMsg.getString("asyncCallId");
                JSONObject jsonData = jsonMsg.getJSONObject("data");
                shown = jsonData.getBoolean("shown");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Runnable runnable = null;
            if (shown) {
                runnable = new Runnable() {
                    public void run() {
                        FrameLayout rootLayout = (FrameLayout)mExtensionContextClient.getActivity()
                                .getWindow().getDecorView().findViewById(android.R.id.content);
                        if (mOverlap) {
                            FrameLayout.LayoutParams params =
                                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                                                                 FrameLayout.LayoutParams.WRAP_CONTENT);
                            params.gravity = (mBannerAtTop ? Gravity.TOP : Gravity.BOTTOM);
                            rootLayout.addView(mAdView, params);
                        } else {
                            mXwalkContent = (LinearLayout)findContainer((ViewGroup)rootLayout);
                            mXwalkContent.setWeightSum(1.0f);
                            View contentView = mXwalkContent.getChildAt(0);
                            LinearLayout.LayoutParams contentViewParam  =
                                    (LinearLayout.LayoutParams)contentView.getLayoutParams();
                            contentViewParam.weight = 1.0f;
                            contentView.setLayoutParams(contentViewParam);
                            LinearLayout.LayoutParams adViewParam =
                                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                  LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
                            mXwalkContent.addView(mAdView,
                                    mBannerAtTop ? 0 : mXwalkContent.getChildCount(), adViewParam);
                        }
                    }
                };
            } else {
                runnable = new Runnable() {
                    public void run() {
                        FrameLayout rootLayout = (FrameLayout)mExtensionContextClient.getActivity()
                                .getWindow().getDecorView().findViewById(android.R.id.content);
                        removeAdView(rootLayout, mAdView);
                    }
                };
            }

            mExtensionContextClient.getActivity().runOnUiThread(runnable);
            Utils.postSuccessMessage(mExtensionClient, mInstanceId, asyncCallId);
        }

        private ViewGroup findContainer(ViewGroup viewGroup) {
            for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                View view = viewGroup.getChildAt(i);
                if (view.getClass().getSimpleName().equals(sContainerClassName))
                    return (ViewGroup)view;
                else if (view instanceof ViewGroup) {
                    ViewGroup viewGroupFound = findContainer((ViewGroup)view);
                    if (null != viewGroupFound && viewGroupFound.getClass().getSimpleName().equals(sContainerClassName))
                        return viewGroupFound;
                }
            }
            return null;
        }

        private void removeAdView(ViewGroup viewGroup, AdView adView) {
            for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                View view = viewGroup.getChildAt(i);
                if (view instanceof AdView) {
                    if ((AdView)view == mAdView) {
                        viewGroup.removeView(view);
                        return;
                    }
                } else if (view instanceof ViewGroup) {
                    removeAdView((ViewGroup)view, adView);
                }
            }
        }
    }

    private class InterstitialAdInstance extends AdInstance {
        private InterstitialAd mInterstitialAd = null;

        public InterstitialAdInstance(XWalkExtensionClient extensionClient,
                          XWalkExtensionContextClient extensionContextClient,
                          int instanceId,
                          int id) {
            super(extensionClient, extensionContextClient, instanceId, id);
        }

        @Override
        protected void create(String message) {
            String asyncCallId = "";
            try {
                JSONObject jsonMsg = new JSONObject(message);
                JSONObject jsonData = jsonMsg.getJSONObject("data");
                mPublisherId = jsonData.getString("publisherId");
                asyncCallId = jsonMsg.getString("asyncCallId");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    mInterstitialAd = new InterstitialAd(mExtensionContextClient.getActivity());
                    mInterstitialAd.setAdUnitId(mPublisherId);
                    mInterstitialAd.setAdListener(createAdListener());
                    AdRequest adRequest = new AdRequest.Builder().build();
                    mInterstitialAd.loadAd(adRequest);
                }
            };

            mExtensionContextClient.getActivity().runOnUiThread(runnable);

            try {
                JSONObject jsonOutput = new JSONObject();
                jsonOutput.put("asyncCallId", asyncCallId);
                jsonOutput.put("cmd", "create_ret");
                jsonOutput.put("error", false);
                JSONObject jsData = new JSONObject();
                jsonOutput.put("data", jsData);
                JSONObject jsBody = new JSONObject();
                jsData.put("id", mId);
                jsData.put("service", "admob");
                mExtensionClient.postMessage(mInstanceId, jsonOutput.toString());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            return;
        }

        @Override
        protected void show(String message) {
            String asyncCallId = "";
            try {
                JSONObject jsonMsg = new JSONObject(message);
                asyncCallId = jsonMsg.getString("asyncCallId");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    if (mInterstitialAd.isLoaded()) {
                      mInterstitialAd.show();
                    }
                }
            };

            mExtensionContextClient.getActivity().runOnUiThread(runnable);
            Utils.postSuccessMessage(mExtensionClient, mInstanceId, asyncCallId);
        }
    }

    protected void create(String message) {
        String type = "";
        String asyncCallId = "";
        try {
            JSONObject jsonMsg = new JSONObject(message);
            asyncCallId = jsonMsg.getString("asyncCallId");
            type = jsonMsg.getJSONObject("data").getString("type");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        AdInstance adInstance = null;
        if (type.equals("banner")) {
            adInstance = new AdViewInstance(mExtensionClient, mExtensionContextClient, mInstanceId, mNextId);
        } else if (type.equals("interstitial")) {
            adInstance = new InterstitialAdInstance(mExtensionClient, mExtensionContextClient, mInstanceId, mNextId);
        } else {
            Utils.postErrorMessage(mExtensionClient, mInstanceId, asyncCallId, "Error: wrong ad type.");
            return;
        }

        mAdInstanceMap.put(mNextId, adInstance);
        ++mNextId;
        adInstance.create(message);
    }

    private void destroy(String message) {
        AdInstance adInstance = getInstanceFromMsg(message);
        if (null != adInstance) {
            adInstance.destroy(message);
        }
    }

    private void show(String message) {
        AdInstance adInstance = getInstanceFromMsg(message);
        if (null != adInstance) {
            adInstance.show(message);
        }
    }

    public AdAdmob(XWalkExtensionClient extensionClient,
                   XWalkExtensionContextClient extensionContextClient,
                   int instanceId) {
        mExtensionClient = extensionClient;
        mExtensionContextClient = extensionContextClient;
        mInstanceId = instanceId;
        mAdInstanceMap = new HashMap<Integer, AdInstance>();
    }

    @Override
    public void handleMessage(String message) {
        String cmd = null;
        try {
            cmd = new JSONObject(message).getString("cmd");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (cmd.equals("create")) {
            create(message);
        } else if (cmd.equals("destroy")) {
            destroy(message);
        } else if (cmd.equals("show")) {
            show(message);
        }
    }
}
