## Introduction
This repository includes external extensions written for Crosswalk Android port.
Fore more details, please refer to https://crosswalk-project.org/#documentation/android_extensions

## Prerequisites
1. gyp
https://code.google.com/p/gyp/
Use `which gyp` to double check.

2. depot_tools
http://dev.chromium.org/developers/how-tos/install-depot-tools
Use `which gclient` to double check.

3. Android SDK
http://developer.android.com/sdk/index.html
Use `which aidl` to double check.

4. Crosswalk
https://crosswalk-project.org/#documentation/downloads
Use `which make_apk.py` to double check.

## Building an external extension
export GYP_GENERATORS='ninja'

gyp --depth=. all.gyp

ninja -C out/Default or for individual target as follows ninja -C out/Default iap

## License
This project's code uses the Apache license, see our `LICENSE.AL2` file.

