package org.eu.droid_ng.wellbeing.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.utils.BuildUtils

class MainSwitchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes),
    CompoundButton.OnCheckedChangeListener {

    private val switchChangeListeners: MutableList<OnMainSwitchChangeListener> = ArrayList()

    @ColorInt
    private var backgroundColor = 0

    @ColorInt
    private var backgroundActivatedColor = 0

    private var textView: TextView? = null

    /**
     * Return the Switch
     */
    var switch: MaterialSwitch? = null
        private set


    private var backgroundOn: Drawable? = null
    private var backgroundOff: Drawable? = null
    private var backgroundDisabled: Drawable? = null
    private var frameView: View? = null

    /**
     * Return the status of the Switch
     */
    /**
     * Update the switch status
     */
    var isChecked: Boolean
        get() = switch?.isChecked == true
        set(checked) {
            switch?.isChecked = checked
            setBackground(checked)
        }

    /**
     * Return the displaying status of org.eu.droid_ng.wellbeing.widget.MainSwitchBar
     */
    val isShowing: Boolean
        get() = visibility == VISIBLE

    init {
        LayoutInflater.from(context).inflate(R.layout.main_switch_bar, this)
        if (!BuildUtils.isAtLeastS()) {
            val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))
            backgroundActivatedColor = a.getColor(0, 0)
            backgroundColor = context.getColor(androidx.appcompat.R.color.material_grey_600)
            a.recycle()
        }
        isFocusable = true
        isClickable = true
        frameView = findViewById(R.id.frame)
        textView = findViewById(R.id.switch_text)
        switch = findViewById(R.id.materialSwitch)
        val switchChecked = switch?.isChecked ?: false
        if (BuildUtils.isAtLeastS()) {
            backgroundOn = ContextCompat.getDrawable(context, R.drawable.main_switch_bar_bg_on)
            backgroundOff = ContextCompat.getDrawable(context, R.drawable.main_switch_bar_bg_off)
            backgroundDisabled = ContextCompat.getDrawable(
                context,
                R.drawable.main_switch_bar_bg_disabled
            )
        }
        addOnSwitchChangeListener { _, isChecked ->
            this@MainSwitchBar.isChecked = isChecked
        }
        if (switch?.visibility == VISIBLE) {
            switch?.setOnCheckedChangeListener(this)
        }
        isChecked = switchChecked

        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                intArrayOf(android.R.attr.text)
            )
            val title = a.getText(0)
            setTitle(title)
            a.recycle()
        }
        setBackground(switchChecked)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        propagateChecked(isChecked)
    }

    override fun performClick(): Boolean {
        switch?.performClick()
        return super.performClick()
    }

    /**
     * Set the title text
     */
    fun setTitle(text: CharSequence?) {
        textView?.text = text
    }

    /**
     * Show the MainSwitchBar
     */
    fun show() {
        visibility = VISIBLE
        switch?.setOnCheckedChangeListener(this)
    }

    /**
     * Hide the MainSwitchBar
     */
    fun hide() {
        if (isShowing) {
            visibility = GONE
            switch?.setOnCheckedChangeListener(null)
        }
    }

    /**
     * Adds a listener for switch changes
     */
    fun addOnSwitchChangeListener(listener: OnMainSwitchChangeListener) {
        if (!switchChangeListeners.contains(listener)) {
            switchChangeListeners.add(listener)
        }
    }

    /**
     * Remove a listener for switch changes
     */
    fun removeOnSwitchChangeListener(listener: OnMainSwitchChangeListener) {
        switchChangeListeners.remove(listener)
    }

    /**
     * Enable or disable the text and switch.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        textView!!.isEnabled = enabled
        switch!!.isEnabled = enabled
        if (BuildUtils.isAtLeastS()) {
            if (enabled) {
                frameView?.background = if (isChecked) backgroundOn else backgroundOff
            } else {
                frameView?.background = backgroundDisabled
            }
        }
    }

    private fun propagateChecked(isChecked: Boolean) {
        setBackground(isChecked)
        val count = switchChangeListeners.size
        for (n in 0 until count) {
            switchChangeListeners[n].onSwitchChanged(switch, isChecked)
        }
    }

    private fun setBackground(isChecked: Boolean) {
        if (!BuildUtils.isAtLeastS()) {
            setBackgroundColor(if (isChecked) backgroundActivatedColor else backgroundColor)
        } else {
            frameView?.background = if (isChecked) backgroundOn else backgroundOff
        }
    }

    internal class SavedState : BaseSavedState {
        var mChecked = false
        var mVisible = false

        constructor(superState: Parcelable?) : super(superState)

        /**
         * Constructor called from [.CREATOR]
         */
        private constructor(input: Parcel) : super(input) {
            mChecked = input.readValue(null) as Boolean
            mVisible = input.readValue(null) as Boolean
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeValue(mChecked)
            out.writeValue(mVisible)
        }

        override fun toString(): String {
            return ("MainSwitchBar.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + mChecked
                    + " visible=" + mVisible + "}")
        }

        object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

        override fun describeContents(): Int {
            return 0
        }
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.mChecked = switch?.isChecked ?: false
        ss.mVisible = isShowing
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        switch?.isChecked = ss.mChecked
        isChecked = ss.mChecked
        setBackground(ss.mChecked)
        visibility = if (ss.mVisible) VISIBLE else GONE
        switch?.setOnCheckedChangeListener(if (ss.mVisible) this else null)
        requestLayout()
    }
}
