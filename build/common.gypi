# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'variables': {
    'variables': {
      'android_sdk_root%': '<!(dirname $(dirname $(dirname $(which aidl))))',
    },
    'android_sdk_root%': '<(android_sdk_root)',
    'android_sdk%': '<!(ls -d <(android_sdk_root)/platforms/*|tail -1)',
    'android_sdk_tools%': '<!(dirname $(which aidl))',
    'app_runtime_java_jar%': '<!(find -L $(dirname $(which make_apk.py)) -name xwalk_app_runtime_java.jar|tail -1)',
  },
}
