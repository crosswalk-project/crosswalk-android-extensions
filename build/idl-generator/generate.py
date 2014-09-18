#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# IDL generator is heavily borrowing code from V8 binding generator
# To make it easier to track changes, we keep most as is in implementing stage
#
# In this script we do following:
# - Copy idl file to binding test idl's dir
# - Call run-bindings-test script, which searches idl's dir for all idls
#   and generate files to results dir
# - Move the generated files (.java and .js) to out dir

# Usage example:
# ./generate.py --idl-file=../../echo/src/org/xwalk/extensions/echo.idl

import argparse
import os
import shutil
import sys


module_path = os.path.dirname(__file__)
script_path = os.path.normpath(os.path.join(module_path,
    'third_party', 'WebKit', 'Tools', 'Scripts'))
sys.path.append(script_path)
from webkitpy.bindings.main import generate_extension_sources


def MakeDirectory(dir_path):
  try:
    os.makedirs(dir_path)
  except OSError:
    pass


def Remove(filename):
  try:
    os.remove(filename)
  except OSError:
    pass


def MoveFile(src, dest):
  if os.path.exists(dest):
    os.remove(dest)
  shutil.move(src, dest)


def main(argv):
  parser = argparse.ArgumentParser()
  parser.add_argument('--component', action='store', dest='component')
  parser.add_argument('--idl-file', action='store', dest='idl_file')
  parser.add_argument('--verbose', action='store_true',
                      default=False, dest='verbose')
  args = parser.parse_args()

  # Create output directory
  out_build_path = os.path.normpath(os.path.join(module_path, os.pardir,
      os.pardir, 'out', 'Default', 'gen', args.component, 'build'))
  out_src_path = os.path.join(out_build_path, 'src')

  # Copy idl file to binding's idl path for source generating
  # e.g. cp ../../echo/src/org/xwalk/extensions/echo.idl \
  #       third_party/WebKit/Source/bindings/tests/idls/modules/
  binding_path = os.path.normpath(os.path.join(module_path,
      'third_party', 'WebKit', 'Source', 'bindings', 'tests'))
  binding_modules_path = os.path.join(binding_path, 'idls', 'modules')
  shutil.copy(args.idl_file, binding_modules_path);

  # Run bindings test script to generate sources
  generate_extension_sources(args.verbose)
  MakeDirectory(out_src_path)

  # Move generated files to out dir
  binding_results_path = os.path.join(binding_path, 'results')
  js_file = args.component + '.js'
  java_file = args.component + '.java'
  js_result = os.path.join(binding_results_path, js_file)
  java_result = os.path.join(binding_results_path, java_file)
  MoveFile(js_result, os.path.join(out_build_path, js_file))
  MoveFile(java_result, os.path.join(out_src_path, java_file))
  Remove(os.path.join(binding_modules_path, os.path.basename(args.idl_file)))


if __name__ == '__main__':
  sys.exit(main(sys.argv))
