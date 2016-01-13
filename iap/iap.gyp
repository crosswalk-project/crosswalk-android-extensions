# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    '../build/common.gypi',
  ],

  'variables': {
    'variables': {
      'build_dir': '<(SHARED_INTERMEDIATE_DIR)/iap/build/',
    },
    'build_dir': '<(build_dir)',
    'aidl_gen_file': '<(build_dir)/com/android/vending/billing/IInAppBillingService.aidl',
    'iap_gen_sources_list': [
      '<(build_dir)/src/InAppPurchaseExtension.java',
      '<(build_dir)/src/InAppPurchaseGoogleHelper.java',
      '<(build_dir)/src/InAppPurchaseHelper.java',
      '<(build_dir)/src/InAppPurchaseXiaoMiHelper.java',
      '<(build_dir)/src/util/Base64.java',
      '<(build_dir)/src/util/Base64DecoderException.java',
      '<(build_dir)/src/util/IabException.java',
      '<(build_dir)/src/util/IabHelper.java',
      '<(build_dir)/src/util/IabResult.java',
      '<(build_dir)/src/util/Inventory.java',
      '<(build_dir)/src/util/Purchase.java',
      '<(build_dir)/src/util/Security.java',
      '<(build_dir)/src/util/SkuDetails.java',
    ],
  },

  'targets': [
    {
      'target_name': 'iap',
      'type': 'none',
      'variables': {
        'has_java_source_list': 1,
        'java_sources_list_input': ['<@(iap_gen_sources_list)'],
        'js_file': '<(DEPTH)/iap/iap.js',
        'json_file': '<(DEPTH)/iap/iap.json',
        'input_jars_paths': [
          '<(core_library_java_jar)',
          '<(android_jar)',
          'libs/SDK_TY_4.4.33.jar',
        ],
      },
      'dependencies': [
        'aidl_iab_service',
      ],
      'includes': [ '../build/java.gypi' ],
    },
    {
      'target_name': 'aidl_iab_service',
      'type': 'none',
      'dependencies': [
        'iab_helper',
      ],
      'variables': {
        'aidl_interface_file': '<(DEPTH)/iap/IInAppBillingServiceInterface.aidl',
      },
      'sources': [
        '<(aidl_gen_file)',
      ],
      'includes': [ '../build/java_aidl.gypi' ],
    },
    {
      'target_name': 'iab_helper',
      'type': 'none',
      'actions': [
        {
          'action_name': 'clone_and_patch_iab_helper',
          'inputs': [
            'ensure_iab_helper.py',
            'iab_helper.patch',
            'IInAppBillingServiceInterface.aidl',
            'src/org/xwalk/extensions/InAppPurchaseExtension.java',
            'src/org/xwalk/extensions/InAppPurchaseGoogleHelper.java',
            'src/org/xwalk/extensions/InAppPurchaseHelper.java',
            'src/org/xwalk/extensions/InAppPurchaseXiaoMiHelper.java',
          ],
          'outputs': [
            '<(aidl_gen_file)',
            '<@(iap_gen_sources_list)',
          ],
          'action': [
            'python', 'ensure_iab_helper.py',
            '--output=<(build_dir)',
          ],
        },
      ],
    },
    {
      'target_name': 'pack_iap_jars',
      'type': 'none',
      'dependencies': [
        'iap',
      ],
      'actions': [
        {
          'action_name': 'combine_jars',
          'inputs': [
            '<(DEPTH)/build/ant_combine.xml',
            'libs/SDK_TY_4.4.33.jar',
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/iap.jar',
          ],
          'action': ['ant',
                     '-f', '<(DEPTH)/build/ant_combine.xml',
                     'combine_jars',
                     '-Ddest.dir=<(SHARED_INTERMEDIATE_DIR)/iap',
                     '-Ddest.file=iap.jar',
                     '-Dextra.jar.dir=<(DEPTH)/iap/libs',
                    ],
        },
      ],
    },
  ],
}
