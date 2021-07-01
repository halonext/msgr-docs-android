package com.kg.kfloatingbutton.lb

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager


/**
 * View that monitors full screen.
 * http://stackoverflow.com/questions/18551135/receiving-hidden-status-bar-entering-a-full-screen-activity-event-on-a-service/19201933#19201933
 */
@SuppressLint("ViewConstructor")
internal class FullscreenObserverView
/**
 * Constructor
 */
    (context: Context,
     /**
      * ScreenListener
      */
     private val mScreenChangedListener: ScreenChangedListener?) : View(context), ViewTreeObserver.OnGlobalLayoutListener, View.OnSystemUiVisibilityChangeListener {

    /**
     * WindowManager.LayoutParams
     */
    /**
     * WindowManager.LayoutParams
     *
     * @return WindowManager.LayoutParams
     */
    val windowLayoutParams :  WindowManager . LayoutParams  =  WindowManager . LayoutParams ()

    /**
     * Last display state (onSystemUiVisibilityChange may not come, so keep it yourself)
     * * If you do not come: Immersive Mode → Touch the status bar → The status bar disappears
     */
    private var mLastUiVisibility: Int = 0

    /**
     * Window Rect
     */
    private val mWindowRect: Rect

    init {

        // Prepare a transparent view with width 1 and height maximum to detect layout changes
        windowLayoutParams.width = 1
        windowLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        windowLayoutParams.type = OVERLAY_TYPE
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        windowLayoutParams.format = PixelFormat.TRANSLUCENT

        mWindowRect = Rect()
        mLastUiVisibility = NO_LAST_VISIBILITY
    } // Set of listeners

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(this)
        setOnSystemUiVisibilityChangeListener(this)
    }

    /**
     * {@inheritDoc}
     */
    override fun onDetachedFromWindow() {
        // Remove layout change notification
        viewTreeObserver.removeOnGlobalLayoutListener(this)
        @Suppress("DEPRECATION")
        setOnSystemUiVisibilityChangeListener ( null )
        super.onDetachedFromWindow()
    }

    /**
     * {@inheritDoc}
     */
    override fun onGlobalLayout() {
        // Get the size of View (full screen)
        if (mScreenChangedListener != null) {
            getWindowVisibleDisplayFrame(mWindowRect)
            mScreenChangedListener.onScreenChanged(mWindowRect, mLastUiVisibility)
        }
    }

    /**
     * It is used in the application that processes the navigation bar (when the onGlobalLayout event does not occur).
     * (Nexus 5 camera app, etc.)
     */
    override fun onSystemUiVisibilityChange(visibility: Int) {
        mLastUiVisibility = visibility
        // Switch between display and non-display in response to changes in the navigation bar
        if (mScreenChangedListener != null) {
            getWindowVisibleDisplayFrame(mWindowRect)
            mScreenChangedListener.onScreenChanged(mWindowRect, visibility)
        }
    }

    companion object {

        /**
         * Constant that mLastUiVisibility does not exist.
         */
        const val NO_LAST_VISIBILITY = -1

        /**
         * Overlay Type
         */
        private val OVERLAY_TYPE = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
    }
}