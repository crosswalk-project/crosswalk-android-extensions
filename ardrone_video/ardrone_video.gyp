# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    '../build/common.gypi',
  ],

  'targets': [
    {
      'target_name': 'ardrone_video',
      'type': 'none',
      'variables': {
        # FIXME(hdq) http://code.google.com/p/gyp/issues/detail?id=112
        # After r1913 merged we can change back to:
        # 'java_in_dir': '.',
        'java_in_dir': '<(DEPTH)/ardrone_video',
        'js_file': 'ardrone_video.js',
        'json_file': 'ardrone_video.json',
        'input_jars_paths': [
          '<(app_runtime_java_jar)',
          '<(android_jar)',
          'libs/aspectjrt-1.7.3.jar',
          'libs/isoparser-1.0-RC-37.jar',
        ],
      },
      'includes': [ '../build/java.gypi' ],
    },
    {
      'target_name': 'pack_jars',
      'type': 'none',
      'dependencies': [
        'ardrone_video',
      ],
      'actions': [
        {
          'action_name': 'combine_jars',
          'inputs': [
            '<(DEPTH)/build/ant_combine.xml',
            'libs/aspectjrt-1.7.3.jar',
            'libs/isoparser-1.0-RC-37.jar',
          ],
          'outputs': [
            # TODO(halton): this is a fake output to avoid loop.
            '<(SHARED_INTERMEDIATE_DIR)/ardrone_video.jar',
          ],
          'action': ['ant',
                     '-f', '<(DEPTH)/build/ant_combine.xml',
                     'combine_jars',
                     '-Ddest.dir=<(SHARED_INTERMEDIATE_DIR)/ardrone_video',
                     '-Ddest.file=ardrone_video.jar',
                     '-Dextra.jar.dir=<(DEPTH)/ardrone_video/libs',
                    ],
        },
      ],
    },
  ],
}
