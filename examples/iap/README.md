# Package for Google Play&XiaoMi Store
1. Install Crosswalk App Tools, see https://github.com/crosswalk-project/crosswalk-app-tools for details.
2. Change ```"xwalk_package_id"``` in manifest.json to the value below:
   * ```"com.crosswalk.iapsample"``` for Google Play.
   * ```"com.sdk.migame.payment"``` for XiaoMi Store.
3. Download the extension zip file from https://github.com/crosswalk-project/crosswalk-android-extensions/releases and unpack it, change ```"xwalk_extensions"``` to the path of the extensions in manifest.json.
4. Add the following additional permissions in the default AndroidManifest.xml:

   ```
   <uses-permission android:name="com.android.vending.BILLING" />
   <uses-permission android:name="android.permission.GET_TASKS"/>
   <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
   <uses-permission android:name="com.xiaomi.sdk.permission.PAYMENT"/>
   ```
5. Run ```crosswalk-pkg```:
    ```
    crosswalk-pkg --targets="arm" --platform="android" --release \
    --crosswalk=/path/to/xwalk_app_template examples/iap
    ```
6. Sign the apk manually, see https://developer.android.com/tools/publishing/app-signing.html#signing-manually for details.
