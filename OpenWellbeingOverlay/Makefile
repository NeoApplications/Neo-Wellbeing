KEY := android
KEYSTORE := ~/.local/publish.keystore
DEFAULT: overlay.apk
clean:
	rm -f overlay.apk overlay.apk.uz overlay.apk.us

overlay.apk.us: res AndroidManifest.xml
	aapt p -M AndroidManifest.xml -S res -I $$ANDROID_SDK_ROOT/platforms/android-29/android.jar -F overlay.apk.us

overlay.apk.uz: overlay.apk.us
	jarsigner -keystore $(KEYSTORE) -signedjar overlay.apk.uz overlay.apk.us $(KEY)

overlay.apk: overlay.apk.uz
	zipalign 4 overlay.apk.uz overlay.apk
