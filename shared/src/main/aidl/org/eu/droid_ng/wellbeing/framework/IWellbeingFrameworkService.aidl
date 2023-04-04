// IWellbeingFrameworkService.aidl
package org.eu.droid_ng.wellbeing.framework;

// Declare any non-default types here with import statements

interface IWellbeingFrameworkService {
    // since 1
    int versionCode() = 0;
    void setAirplaneMode(boolean value) = 1;

    // since 2
    void onNotificationPosted(String packageName) = 2;
    long getEventCount(String type, int dimension, long from, long to) = 3;
    Map getTypesForPrefix(String prefix, int dimension, long from, long to) = 4;
}