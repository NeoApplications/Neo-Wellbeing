package org.eu.droid_ng.wellbeing.widget

import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Called when the checked state of the Switch has changed.
 */
fun interface OnMainSwitchChangeListener {
    /**
     * @param switchView The Switch view whose state has changed.
     * @param isChecked  The new checked state of switchView.
     */
    fun onSwitchChanged(switchView: MaterialSwitch?, isChecked: Boolean)
}
