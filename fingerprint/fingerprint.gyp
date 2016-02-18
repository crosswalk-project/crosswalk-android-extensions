# Copyright (c) 2016 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    '../build/common.gypi',
  ],

  'targets': [
    {
      'target_name': 'fingerprint',
      'type': 'none',
      'variables': {
        'java_in_dir': '<(DEPTH)/fingerprint',
        'js_file': 'fingerprint.js',
        'json_file': 'fingerprint.json',
        'input_jars_paths': [
          '<(core_library_java_jar)',
          '<(android_jar)',
        ],
      },
      'includes': [ '../build/java.gypi' ],
    },
  ],
}
