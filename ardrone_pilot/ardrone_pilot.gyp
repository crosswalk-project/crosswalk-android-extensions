# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    '../build/common.gypi',
  ],

  'targets': [
    {
      'target_name': 'ardrone_pilot',
      'type': 'none',
      'variables': {
        'java_in_dir': '.',
        'js_file': 'ardrone_pilot.js',
        'json_file': 'ardrone_pilot.json',
        'input_jars_paths': [
          '<(app_runtime_java_jar)',
          '<(android_jar)',
        ],
      },
      'includes': [ '../build/java.gypi' ],
    },
  ],
}
