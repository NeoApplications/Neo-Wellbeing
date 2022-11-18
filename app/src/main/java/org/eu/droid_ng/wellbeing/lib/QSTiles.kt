package org.eu.droid_ng.wellbeing.lib

import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import org.eu.droid_ng.wellbeing.R

class FocusModeQSTile : TileService() {
	override fun onStartListening() {
		super.onStartListening()
		val tw = WellbeingService.get()
		val state = tw.getState()

		val tile = qsTile
		tile.state = if (state.isFocusModeEnabled()) STATE_ACTIVE else STATE_INACTIVE
		tile.subtitle = getString(if (state.isFocusModeEnabled()) R.string.on else R.string.off)
		tile.updateTile()
	}

	override fun onClick() {
		super.onClick()

		val tw = WellbeingService.get()
		val state = tw.getState()
		if (state.isFocusModeEnabled())
			tw.disableFocusMode()
		else
			tw.enableFocusMode()
	}
}

class BedtimeModeQSTile : TileService() {
	override fun onStartListening() {
		super.onStartListening()
		val tw = WellbeingService.get()
		val state = tw.getState()

		val tile = qsTile
		tile.state = if (state.isBedtimeModeEnabled()) STATE_ACTIVE else STATE_INACTIVE
		tile.subtitle = getString(if (state.isBedtimeModeEnabled()) R.string.on else R.string.off)
		tile.updateTile()
	}

	override fun onClick() {
		super.onClick()

		val tw = WellbeingService.get()
		val state = tw.getState()
		tw.setBedtimeMode(!state.isBedtimeModeEnabled())
	}
}