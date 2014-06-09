# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'variables': {
    'variables': {
      'variables': {
        'android_version%': '<!(basename $(dirname $(which aidl))|cut -d. -f1)',
      },
      'android_sdk_root%': '<!(dirname $(dirname $(dirname $(which aidl))))',
      'android_sdk_version%': '<(android_version)',
      'android_sdk_build_tools_version%': '<(android_version).0.0',
    },
    'android_sdk%': '<(android_sdk_root)/platforms/android-<(android_sdk_version)',
    'android_sdk_tools%': '<(android_sdk_root)/build-tools/<(android_sdk_build_tools_version)',
  },
}
