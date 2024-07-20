package org.eu.droid_ng.wellbeing.utils

import android.os.Build

object BuildUtils {

    fun isAtLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
