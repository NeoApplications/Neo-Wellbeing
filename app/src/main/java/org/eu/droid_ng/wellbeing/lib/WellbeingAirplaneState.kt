package org.eu.droid_ng.wellbeing.lib

import android.content.Context
import android.provider.Settings

enum class WellbeingAirplaneState(val airplaneModeState: Boolean, val systemAirplaneModeState: Boolean, val wellbeingAirplaneModeState: Boolean) {
    DISABLED_BY_SYSTEM(false, false, false) {
        override fun onReceiveAirplaneEnabled(): WellbeingAirplaneState {
            return ENABLED_BY_SYSTEM
        }

        override fun onEnableAirplaneByWellbeing(): WellbeingAirplaneState {
            return ENABLED_DESPITE_SYSTEM
        }
    },
    ENABLED_BY_SYSTEM(true, true, false) {
        override fun onReceiveAirplaneDisabled(): WellbeingAirplaneState {
            return DISABLED_BY_SYSTEM
        }

        override fun onEnableAirplaneByWellbeing(): WellbeingAirplaneState {
            return ENABLED_WITH_SYSTEM
        }
    },
    ENABLED_WITH_SYSTEM(true, true, true) {
        override fun onReceiveAirplaneDisabled(): WellbeingAirplaneState {
            return DISABLED_DESPITE_WELLBEING_SYSTEM
        }

        override fun onDisableAirplaneByWellbeing(): WellbeingAirplaneState {
            return ENABLED_BY_SYSTEM
        }
    },
    ENABLED_DESPITE_SYSTEM(true, false, true) {
        override fun onReceiveAirplaneDisabled(): WellbeingAirplaneState {
            return DISABLED_DESPITE_WELLBEING
        }

        override fun onDisableAirplaneByWellbeing(): WellbeingAirplaneState {
            return DISABLED_BY_SYSTEM
        }
    },
    DISABLED_DESPITE_WELLBEING_SYSTEM(false, false, true) {
        override fun onReceiveAirplaneEnabled(): WellbeingAirplaneState {
            return ENABLED_WITH_SYSTEM
        }

        override fun onDisableAirplaneByWellbeing(): WellbeingAirplaneState {
            return DISABLED_BY_SYSTEM
        }
    },
    DISABLED_DESPITE_WELLBEING(false, false, true) {
        override fun onReceiveAirplaneEnabled(): WellbeingAirplaneState {
            return ENABLED_DESPITE_SYSTEM
        }

        override fun onDisableAirplaneByWellbeing(): WellbeingAirplaneState {
            return DISABLED_BY_SYSTEM
        }
    };

    open fun onDisableAirplaneByWellbeing(): WellbeingAirplaneState {
        return this
    }

    open fun onEnableAirplaneByWellbeing(): WellbeingAirplaneState {
        return this
    }

    open fun onReceiveAirplaneDisabled(): WellbeingAirplaneState {
        return this
    }

    open fun onReceiveAirplaneEnabled(): WellbeingAirplaneState {
        return this
    }

    open fun shouldRestoreAirplaneMode(): Boolean {
        return this.airplaneModeState != this.systemAirplaneModeState
    }

    companion object {
        fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.Global.getInt(context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        }
    }
}