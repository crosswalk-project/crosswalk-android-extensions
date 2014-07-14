#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

import optparse
import os
import shutil
import sys


def TryAddDepotToolsToPythonPath():
  depot_tools = FindDepotToolsInPath()
  if depot_tools:
    sys.path.append(depot_tools)


def FindDepotToolsInPath():
  paths = os.getenv('PATH').split(os.path.pathsep)
  for path in paths:
    if os.path.basename(path) == '':
      # path is end with os.path.pathsep
      path = os.path.dirname(path)
    if os.path.basename(path) == 'depot_tools':
      return path
  return None


"""
Will generate following dir structure:

  out/Default/gen/iap/build  # output_dir
    +-- marketbilling        # repo_dir
    +-- src                  # src_dir
         +-- iap.java
         +-- util            # patched iab helper utils
        
iab helper repo from Google:
1. If the iap dir is not exist or not repo, git clone a new one
2. Set version
"""
def EnsureAndEnterIABRepo(output_dir, repo):
  src_dir = os.path.join(output_dir, 'src')
  if not os.path.exists(src_dir):
    os.makedirs(src_dir)

  repo_dir = os.path.join(output_dir, repo)

  git_url = 'https://code.google.com/p/%s/' % repo
  co = GitCheckout(repo_dir, None, 'master', git_url, None)
  co.prepare('ff1c062b22b97c3baee12eae8d65a2adcf396f5f')

  os.chdir(repo_dir)
  return src_dir


def main():
  parser = optparse.OptionParser()
  info = ('The output directory for iab helper')
  parser.add_option('--output', help=info)
  options, _ = parser.parse_args()

  root_dir = os.getcwd()
  output_dir = os.path.join(root_dir, options.output)
  src_dir = EnsureAndEnterIABRepo(output_dir, 'marketbilling')

  #TODO(hdq): use depot_tools util to patch multiple files when it's ready
  os.system('git am < %s/iab_helper.patch --quiet' % root_dir)

  util_src = os.path.join('v3', 'src', 'com', 'example',
                          'android', 'trivialdrivesample', 'util')
  util_dst = os.path.join(src_dir, 'util')
  shutil.rmtree(util_dst)
  shutil.copytree(util_src, util_dst)

  aidl = os.path.join('com', 'android', 'vending', 'billing',
                      'IInAppBillingService.aidl')
  shutil.copy(os.path.join('v3', 'src', '%s' % aidl),
              os.path.join(output_dir, aidl))
  shutil.copy(os.path.join(root_dir, 'src', 'org', 'xwalk', 'extensions',
                           'iap.java'), src_dir)
  return 0


TryAddDepotToolsToPythonPath()
try:
  from checkout import GitCheckout
except ImportError:
  sys.stderr.write("Can't find gclient_utils, please add depot_tools to PATH\n")
  sys.exit(1)

if __name__ == '__main__':
  sys.exit(main())
