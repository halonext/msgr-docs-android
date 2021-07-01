package com.kg.kfloatingbutton.lb

import android.graphics.Rect

/**
 * A listener that handles screen changes.
 */
internal interface ScreenChangedListener {
    /**
     * Called when the screen changes.
     *
     * @param windowRect System window rect
     * @param visibility System UI Mode
     */
    fun onScreenChanged(windowRect: Rect, visibility: Int)
}
