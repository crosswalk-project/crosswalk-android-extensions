How to package the ad example?

1. Replace 'MY_PUBID' in ad.html with your publish id.
2. python make_apk.py --package=org.xwalk.extensions.ad  \
   --extensions="./out/Default/gen/ad/" \
   --manifest=./examples/ad/ad.json \
   --arch=x86
