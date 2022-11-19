// IWellbeingFrameworkService.aidl
package org.eu.droid_ng.wellbeing.lib;

// Declare any non-default types here with import statements

interface IWellbeingFrameworkService {
    int versionCode() = 0;

    void setAirplaneMode(boolean value) = 1;
}