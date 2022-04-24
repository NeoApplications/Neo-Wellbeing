overlay.apk.uz: res AndroidManifest.xml
	aapt p -M AndroidManifest.xml -S res -I $$ANDROID_SDK_ROOT/platforms/android-28/android.jar -F overlay.apk.uz

overlay.apk.us: overlay.apk.uz
	zipalign 4 overlay.apk.uz overlay.apk.us
