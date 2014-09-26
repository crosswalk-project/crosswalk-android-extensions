# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    'build/common.gypi',
  ],

  'targets': [
    {
      'target_name': 'build_all_android_extensions',
      'type': 'none',
      'dependencies': [
        'ardrone_pilot/ardrone_pilot.gyp:*',
        'ardrone_video/ardrone_video.gyp:*',
        'iap/iap.gyp:*',
        'idl_demo/idl_demo.gyp:*',
      ],
    },
  ],
}
