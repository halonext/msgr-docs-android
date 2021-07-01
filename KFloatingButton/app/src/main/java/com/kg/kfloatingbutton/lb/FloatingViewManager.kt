package com.kg.kfloatingbutton.lb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import java.util.*

/**
 * A class that handles FloatingView.
 * TODO: The operation is jerky, so look for the cause
 * TODO: Multiple display support to follow movement is supported in the second bullet
 */
class FloatingViewManager
/**
 * Constructor
 *
 * @param context  Context
 * @param listener FloatingViewListener
 */
    (
    /**
     * [Context]
     */
    private val mContext: Context,
    /**
     * FloatingViewListener
     */
    private val mFloatingViewListener: FloatingViewListener?) : ScreenChangedListener, View.OnTouchListener, TrashViewListener, FloatingView.RealTimePositionListener {

    /**
     * [Resources]
     */
    private val mResources = mContext.resources

    /**
     * WindowManager
     */
    private val mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * [DisplayMetrics]
     */
    private val mDisplayMetrics = DisplayMetrics()

    /**
     * FloatingView in operating state
     */
    private var mTargetFloatingView: FloatingView? = null

    /**
     * View that monitors full screen.
     */
    private val mFullscreenObserverView: FullscreenObserverView

    /**
     * A View that removes the FloatingView.
     */
    private val mTrashView: TrashView

    /**
     * FloatingView collision detection rectangle
     */
    private val mFloatingViewRect = Rect()

    /**
     * TrashView collision detection rectangle
     */
    private val mTrashViewRect = Rect()

    /**
     * Flag to allow touch movement
     * This is a flag to prevent touch processing from being accepted when the screen is rotated.
     */
    private var mIsMoveAccept: Boolean = false

    /**
     * Current display mode
     */
    @DisplayMode
    private var mDisplayMode: Int = 0

    /**
     * Cutout safe inset rect
     */
    private val mSafeInsetRect: Rect

    /**
     * List of FloatingViews pasted into the Window
     * TODO: Will make sense in multiple displays of the second Floating View
     */
    private val mFloatingViewList: ArrayList<FloatingView>

    /**
     * Check if it overlaps with the deleted View.
     *
     * @return true if it overlaps with the deleted View
     */
    private // If it is invalid, no overlap judgment is performed.
    // INFO: TrashView and FloatingView must be the same Gravity
    val isIntersectWithTrash: Boolean
        get() {
            if (!mTrashView.isTrashEnabled) {
                return false
            }
            mTrashView.getWindowDrawingRect(mTrashViewRect)
            mTargetFloatingView!!.getWindowDrawingRect(mFloatingViewRect)
            return Rect.intersects(mTrashViewRect, mFloatingViewRect)
        }

    /**
     * Gets the display / non-display status of TrashView.
     *
     * @return If true, display state (overlap judgment is enabled)
     */
    /**
     * Set the display / non-display of TrashView.
     *
     * @param enabled Display if true
     */
    var isTrashViewEnabled :  Boolean
        get() = mTrashView.isTrashEnabled
        set(enabled) {
            mTrashView.isTrashEnabled = enabled
        }

    /**
     * Display mode
     */
    @IntDef(DISPLAY_MODE_SHOW_ALWAYS, DISPLAY_MODE_HIDE_ALWAYS, DISPLAY_MODE_HIDE_FULLSCREEN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DisplayMode

    /**
     * Moving direction
     */
    @IntDef(MOVE_DIRECTION_DEFAULT, MOVE_DIRECTION_LEFT, MOVE_DIRECTION_RIGHT, MOVE_DIRECTION_NEAREST, MOVE_DIRECTION_NONE, MOVE_DIRECTION_THROWN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class MoveDirection

    init {
        mIsMoveAccept =  false
        mDisplayMode = DISPLAY_MODE_HIDE_FULLSCREEN
        mSafeInsetRect = Rect()

        // Build a View that works with the Floating View
        mFloatingViewList = ArrayList()
        mFullscreenObserverView = FullscreenObserverView(mContext, this)
        mTrashView = TrashView(mContext)
        //mTrashView.setTrashEnabled(false);
    }

    /**
     * Hide the View when the screen goes full screen.
     */
    override fun onScreenChanged(windowRect: Rect, visibility: Int) {
        // detect status bar
        val isFitSystemWindowTop = windowRect.top ==  0
        val isHideStatusBar: Boolean
        isHideStatusBar = isFitSystemWindowTop

        // detect navigation bar
        val isHideNavigationBar = if (visibility == FullscreenObserverView.NO_LAST_VISIBILITY) {
            // At the first it can not get the correct value, so do special processing
            mWindowManager.defaultDisplay.getRealMetrics(mDisplayMetrics)
            windowRect.width() - mDisplayMetrics.widthPixels == 0 && windowRect.bottom - mDisplayMetrics.heightPixels == 0
        } else {
            visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        val isPortrait = mResources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        // update FloatingView layout
        mTargetFloatingView!!.onUpdateSystemLayout(isHideStatusBar, isHideNavigationBar, isPortrait, windowRect)

        // Do nothing unless in full screen hidden mode
        if (mDisplayMode != DISPLAY_MODE_HIDE_FULLSCREEN) {
            return
        }

        mIsMoveAccept =  false
        val state = mTargetFloatingView!!.state
        // Hide all if they do not overlap
        if (state == FloatingView.STATE_NORMAL) {
            val size = mFloatingViewList.size
            for (i in 0 until size) {
                val floatingView = mFloatingViewList[i]
                floatingView.visibility = if (isFitSystemWindowTop) View.GONE else View.VISIBLE
            }
            mTrashView.dismiss()
        } else if (state == FloatingView.STATE_INTERSECTING) {
            mTargetFloatingView!!.setFinishing()
            mTrashView.dismiss()
        } // Delete if overlapping
    }

    /**
     * Update ActionTrashIcon
     */
    override fun onUpdateActionTrashIcon() {
        mTrashView.updateActionTrashIcon(mTargetFloatingView!!.measuredWidth.toFloat(), mTargetFloatingView!!.measuredHeight.toFloat(), mTargetFloatingView!!.shape)
    }

    /**
     * Lock the touch of the FloatingView.
     */
    override fun onTrashAnimationStarted(@TrashView.AnimationState animationCode: Int) {
        // Do not touch all floating views in case of close or forced close
        if (animationCode == TrashView.ANIMATION_CLOSE || animationCode == TrashView.ANIMATION_FORCE_CLOSE) {
            val size = mFloatingViewList.size
            for (i in 0 until size) {
                val floatingView = mFloatingViewList[i]
                floatingView.setDraggable(false)
            }
        }
    }

    /**
     * Releases the touch lock of FloatingView.
     */
    override fun onTrashAnimationEnd(@TrashView.AnimationState animationCode: Int) {

        val state = mTargetFloatingView!!.state
        // Delete View when finished
        if (state == FloatingView.STATE_FINISHING) {
            removeViewToWindow(mTargetFloatingView!!)
        }

        // Return the touch state of all FloatingViews
        val size = mFloatingViewList.size
        for (i in 0 until size) {
            val floatingView = mFloatingViewList[i]
            floatingView.setDraggable(true)
        }

    }

    /**
     * Processes the display / non-display of the delete button.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action

        // Do nothing if move permission is not given even though it is not in the pressed state (corresponding to the phenomenon that ACTION_MOVE comes immediately after rotation and FloatingView disappears)
        if (action != MotionEvent.ACTION_DOWN && !mIsMoveAccept) {
            return false
        }

        val state = mTargetFloatingView!!.state
        mTargetFloatingView = v as  FloatingView

        // Press
        if (action == MotionEvent.ACTION_DOWN) {
            // No processing
            mIsMoveAccept = true
        } else if (action == MotionEvent.ACTION_MOVE) {
            // This state
            val isIntersecting = isIntersectWithTrash
            // State so far
            val isIntersect = state == FloatingView.STATE_INTERSECTING
            // Make FloatingView follow TrashView if they overlap
            if (isIntersecting) {
                mTargetFloatingView!!.setIntersecting(mTrashView.trashIconCenterX.toInt(), mTrashView.trashIconCenterY.toInt())
            }
            // If it starts to overlap
            if (isIntersecting && !isIntersect) {
                mTargetFloatingView!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                mTrashView.setScaleTrashIcon(true)
            } else if (!isIntersecting && isIntersect) {
                mTargetFloatingView!!.setNormal()
                mTrashView.setScaleTrashIcon ( false )
            } // In case of the end of overlap
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // If they overlap
            if (state == FloatingView.STATE_INTERSECTING) {
                // Delete FloatingView and cancel the enlarged state
                mTargetFloatingView!!.setFinishing()
                mTrashView.setScaleTrashIcon ( false )
            }
            mIsMoveAccept =  false

            // Touch finish callback
            if (mFloatingViewListener != null) {
                rewritePositionY()
            }
        } // Push up, cancel
        // Move

        // Notify TrashView of events
        // Pass the finger position in the normal state
        // Pass the location of TrashView if they overlap
        if (state == FloatingView.STATE_INTERSECTING) {
            mTrashView.onTouchFloatingView(event, mFloatingViewRect.left.toFloat(), mFloatingViewRect.top.toFloat())
        } else {
            val params = mTargetFloatingView!!.windowLayoutParams
            mTrashView.onTouchFloatingView(event, params.x.toFloat(), params.y.toFloat())
        }

        return false
    }

    /**
     * Set the image of the fixed delete icon.
     *
     * @param resId drawable ID
     */
    fun setFixedTrashIconImage(@DrawableRes resId: Int) {
        mTrashView.setFixedTrashIconImage(resId)
    }

    /**
     * Set the image of the delete icon to take action.
     *
     * @param resId drawable ID
     */
    fun setActionTrashIconImage(@DrawableRes resId: Int) {
        mTrashView.setActionTrashIconImage(resId)
    }

    /**
     * Set the fixed delete icon.
     *
     * @param drawable Drawable
     */
    fun setFixedTrashIconImage(drawable: Drawable) {
        mTrashView.setFixedTrashIconImage(drawable)
    }

    /**
     * Set the delete icon for action.
     *
     * @param drawable Drawable
     */
    fun setActionTrashIconImage(drawable: Drawable) {
        mTrashView.setActionTrashIconImage(drawable)
    }

    /**
     * Change the display mode.
     *
     * @param displayMode [.DISPLAY_MODE_SHOW_ALWAYS] or [.DISPLAY_MODE_HIDE_ALWAYS] or [.DISPLAY_MODE_HIDE_FULLSCREEN]
     */
    fun setDisplayMode(@DisplayMode displayMode: Int) {
        mDisplayMode = displayMode
        // Always show / Hide at full screen mode
        if (mDisplayMode == DISPLAY_MODE_SHOW_ALWAYS || mDisplayMode == DISPLAY_MODE_HIDE_FULLSCREEN) {
            for (floatingView in mFloatingViewList) {
                floatingView.visibility = View.VISIBLE
            }
        } else if (mDisplayMode == DISPLAY_MODE_HIDE_ALWAYS) {
            for (floatingView in mFloatingViewList) {
                floatingView.visibility = View.GONE
            }
            mTrashView.dismiss()
        } // In the mode to always hide
    }

    /**
     * Set the DisplayCutout's safe area
     * Note:You must set the Cutout obtained on portrait orientation.
     *
     * @param safeInsetRect DisplayCutout#getSafeInsetXXX
     */
    fun setSafeInsetRect(safeInsetRect: Rect?) {
        if (safeInsetRect == null) {
            mSafeInsetRect.setEmpty()
        } else {
            mSafeInsetRect.set(safeInsetRect)
        }

        val size = mFloatingViewList.size
        if (size == 0) {
            return
        }

        // update floating view
        for (i in 0 until size) {
            val floatingView = mFloatingViewList[i]
            floatingView.setSafeInsetRect(mSafeInsetRect)
        }
        // dirty hack
        mFullscreenObserverView.onGlobalLayout()
    }

    /**
     * Paste the View into the Window.
     *
     * @param view Floating View
     * @param options Options
     */
    @SuppressLint("ClickableViewAccessibility")
    fun addViewToWindow(view: View, options: Options) {
        val isFirstAttach = mFloatingViewList.isEmpty()
        // FloatingView
        val floatingView = FloatingView(mContext)
        floatingView.setInitCoords(options.floatingViewX, options.floatingViewY)
        floatingView.setOnTouchListener(this)
        floatingView.setRealTimePositionListener(this)
        floatingView.shape = options.shape
        floatingView.setOverMargin(options.overMargin)
        floatingView.setMoveDirection(options.moveDirection)
        floatingView.usePhysics(options.usePhysics)
        floatingView.setAnimateInitialMove(options.animateInitialMove)
        floatingView.setSafeInsetRect(mSafeInsetRect)

        // set FloatingView size
        val targetParams = FrameLayout.LayoutParams(options.floatingViewWidth, options.floatingViewHeight)
        view.layoutParams = targetParams
        floatingView.addView(view)

        // In hidden mode
        if (mDisplayMode == DISPLAY_MODE_HIDE_ALWAYS) {
            floatingView.visibility = View.GONE
        }
        mFloatingViewList.add(floatingView)
        // TrashView
        mTrashView.setTrashViewListener(this)

        // Paste View
        mWindowManager.addView(floatingView, floatingView.windowLayoutParams)
        // Paste the full screen monitor view and delete view only when first pasting
        if (isFirstAttach) {
            mWindowManager.addView(mFullscreenObserverView, mFullscreenObserverView.windowLayoutParams)
            mTargetFloatingView = floatingView
        } else {
            removeViewImmediate(mTrashView)
        }
        // I want you to come to the top, so paste it every time
        mWindowManager.addView(mTrashView, mTrashView.windowLayoutParams)
    }

    /**
     * Remove the View from the Window.
     *
     * @param floatingView FloatingView
     */
    private fun removeViewToWindow(floatingView: FloatingView) {
        val matchIndex = mFloatingViewList.indexOf(floatingView)
        // Show and remove from list if found
        if (matchIndex != -1) {
            removeViewImmediate(floatingView)
            mFloatingViewList.removeAt(matchIndex)
        }

        // Check the remaining views
        if (mFloatingViewList.isEmpty()) {
            // Notify the end
            mFloatingViewListener?.onFinishFloatingView()
        }
    }

    /**
     * Remove all Views from the Window.
     */
    fun removeAllViewToWindow() {
        removeViewImmediate(mFullscreenObserverView)
        removeViewImmediate(mTrashView)
        // Delete FloatingView
        val size = mFloatingViewList.size
        for (i in 0 until size) {
            val floatingView = mFloatingViewList[i]
            removeViewImmediate(floatingView)
        }
        mFloatingViewList.clear()
    }

    /**
     * Safely remove the View (issue #89)
     *
     * @param view [View]
     */
    private fun removeViewImmediate(view: View) {
        // fix #100(crashes on Android 8)
        try {
            mWindowManager.removeViewImmediate(view)
        } catch (e: IllegalArgumentException) {
            //do nothing
        }

    }

    /**
     * A class that represents options when pasting FloatingView.
     */
    class Options {

        /**
         * Floating View rectangle (SHAPE_RECTANGLE or SHAPE_CIRCLE)
         */
        var shape: Float = 0.toFloat()

        /**
         * Off-screen overhang margin (px)
         */
        var overMargin :  Int  =  0

        /**
         * X coordinate of Floating View with the origin at the bottom left of the screen
         */
        var floatingViewX: Int = 0

        /**
         * Y coordinate of Floating View with the origin at the bottom left of the screen
         */
        var floatingViewY: Int = 0

        /**
         * Width of FloatingView(px)
         */
        var floatingViewWidth: Int = 0

        /**
         * Height of FloatingView(px)
         */
        var floatingViewHeight: Int = 0

        /**
         * Direction of Floating View adsorption
         * * If you specify the coordinates, it will automatically become MOVE_DIRECTION_NONE.
         */
        @MoveDirection
        var moveDirection :  Int  =  0

        /**
         * Use of physics-based animations or (default) ValueAnimation
         */
        var usePhysics: Boolean = false

        /**
         * Flags that animate during initial display
         */
        var animateInitialMove: Boolean = false

        /**
         * Set the default value for the option.
         */
        init {
            shape = SHAPE_CIRCLE
            overMargin =  0
            floatingViewX = FloatingView.DEFAULT_X
            floatingViewY = FloatingView.DEFAULT_Y
            floatingViewWidth = FloatingView.DEFAULT_WIDTH
            floatingViewHeight = FloatingView.DEFAULT_HEIGHT
            moveDirection = MOVE_DIRECTION_DEFAULT
            usePhysics = true
            animateInitialMove = true
        }

    }

    override fun currentPosition() {
        rewritePositionY()
    }

    private fun rewritePositionY() {
        val isFinishing = mTargetFloatingView!!.state == FloatingView.STATE_FINISHING
        val params = mTargetFloatingView!!.windowLayoutParams
        mFloatingViewListener!!.onTouchFinished(isFinishing, params.x, mTargetFloatingView!!.heightLimit - params.y)
    }

    companion object {

        /**
         * Always display mode
         */
        const val DISPLAY_MODE_SHOW_ALWAYS = 1

        /**
         * Always hide mode
         */
        const val DISPLAY_MODE_HIDE_ALWAYS = 2

        /**
         * Mode to hide in full screen
         */
        const val DISPLAY_MODE_HIDE_FULLSCREEN = 3

        /**
         * Move to the left and right
         */
        const val MOVE_DIRECTION_DEFAULT = 0
        /**
         * Always move to the left
         */
        const val MOVE_DIRECTION_LEFT = 1
        /**
         * Always move to the right
         */
        const val MOVE_DIRECTION_RIGHT = 2

        /**
         * Do not move
         */
        const val MOVE_DIRECTION_NONE = 3

        /**
         * Move in the direction closer to the side
         */
        const val MOVE_DIRECTION_NEAREST = 4

        /**
         * Goes in the direction in which it is thrown
         */
        const val MOVE_DIRECTION_THROWN = 5

        /**
         * When the shape of View is circular
         */
        const val SHAPE_CIRCLE = 1.0f

        /**
         * When the shape of View is square
         */
        const val SHAPE_RECTANGLE = 1.4142f

        /**
         * Find the safe area of DisplayCutout.
         *
         * @param activity [Activity] (Portrait and `windowLayoutInDisplayCutoutMode` != never)
         * @return Safe cutout insets.
         */
        fun findCutoutSafeArea(activity: Activity): Rect {
            val safeInsetRect = Rect()
            // TODO:Rewrite with android-x
            // TODO:Consider alternatives
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return safeInsetRect
            } else {
                // Fix: getDisplayCutout() on a null object reference (issue #110)
                val windowInsets = activity.window.decorView.rootWindowInsets ?: return safeInsetRect

                // set safeInsetRect
                val displayCutout = windowInsets.displayCutout
                if (displayCutout != null) {
                    safeInsetRect.set(displayCutout.safeInsetLeft, displayCutout.safeInsetTop, displayCutout.safeInsetRight, displayCutout.safeInsetBottom)
                }
            }
            return safeInsetRect
        }
    }
}