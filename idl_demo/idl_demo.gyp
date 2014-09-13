# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'includes': [
    '../build/common.gypi',
  ],

  # TODO(hdq): this gyp is quite the same for different extensions,
  # consider to move some parts into common.gypi
  'variables': {
    'variables': {
      'variables': {
        'component': 'idl_demo',
      },
      'component': '<(component)',
      'gen_build_dir': '<(SHARED_INTERMEDIATE_DIR)/<(component)/build',
      'gen_src_dir': '<(SHARED_INTERMEDIATE_DIR)/<(component)/build/src',
    },
    'component': '<(component)',
    'gen_build_dir': '<(gen_build_dir)',
    'gen_src_dir': '<(gen_src_dir)',
    'gen_js_file': '<(gen_build_dir)/<(component).js',
    'gen_java_file': '<(gen_src_dir)/<(component).java',
    'idl_file': '<(component).idl',
    'java_source_list': [
      '<(gen_java_file)',
      'src/org/xwalk/extensions/<(component)_impl.java',
    ],
  },

  'targets': [
    {
      'target_name': '<(component)',
      'type': 'none',
      'dependencies': [ 'idl_generator' ],
      'variables': {
        'has_java_source_list': 1,
        'java_sources_list_input': ['<@(java_source_list)'],
        'js_file': '<(gen_js_file)',
        'json_file': '<(component).json',
        'input_jars_paths': [
          '<!(dirname $(which make_apk.py))/libs/xwalk_app_runtime_java.jar',
          '<(android_jar)',
        ],
      },
      'includes': [ '../build/java.gypi' ],
    },
    {
      'target_name': 'idl_generator',
      'type': 'none',
      'actions': [
        {
          'action_name': 'generate_by_idl',
          'inputs': [
            '../build/idl-generator/generate.py',
            '<(idl_file)',
          ],
          'outputs': [
            '<(gen_js_file)',
            '<(gen_java_file)',
          ],
          'action': [
            'python', '../build/idl-generator/generate.py',
            '--component=<(component)',
            '--idl-file=<(idl_file)',
          ],
        },
      ],
    }
  ],
}
