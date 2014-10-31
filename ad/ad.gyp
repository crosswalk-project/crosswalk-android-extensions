# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes':[
    '../build/common.gypi',
  ],

  'targets': [
    {
      'target_name': 'ad',
      'type': 'none',
      'variables': {
        'java_in_dir': '.',
        'js_file': 'ad.js',
        'json_file': 'ad.json',
        'input_jars_paths': [
          '<(app_runtime_java_jar)',
          '<(android_jar)',
          '<(android_sdk_root)/extras/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar',
        ],
      },
      'includes': [ '../build/java.gypi' ],
    },
    {
      'target_name': 'pack_jars',
      'type': 'none',
      'dependencies': [
        'ad',
      ],
      'actions': [
        {
          'action_name': 'combine_jars',
          'inputs': [
            '<(DEPTH)/build/ant_combine.xml',
            '<(android_sdk_root)/extras/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar',
          ],
          'outputs': [
            # TODO(shawn): this is a fake output to avoid loop.
            '<(SHARED_INTERMEDIATE_DIR)/ad.jar',
          ],
          'action': ['ant',
                     '-f', '<(DEPTH)/build/ant_combine.xml',
                     'combine_jars',
                     '-Ddest.dir=<(SHARED_INTERMEDIATE_DIR)/ad',
                     '-Ddest.file=ad.jar',
                     '-Dextra.jar.dir=<(android_sdk_root)/extras/google/google_play_services/libproject/google-play-services_lib/libs',
                    ],
        },
        {
          'action_name': 'customize_manifest',
          'inputs': [
            '<(DEPTH)/ad/AndroidManifest.xml'
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/ad/AndroidManifest.xml',
          ],
          'action': ['python',
                     '<(DEPTH)/ad/customize_manifest.py',
                     '<(DEPTH)/ad/AndroidManifest.xml',
                     '<(SHARED_INTERMEDIATE_DIR)/ad/AndroidManifest.xml',
                    ],
        },
      ],
    },
  ],
}
