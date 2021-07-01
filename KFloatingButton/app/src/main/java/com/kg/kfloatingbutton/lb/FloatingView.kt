package com.kg.kfloatingbutton.lb

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.*
import java.lang.ref.WeakReference
import kotlin.math.*

/**
 * A class that represents a floating view.
 * http://stackoverflow.com/questions/18503050/how-to-create-draggabble-system-alert-in-android
 * FIXME: In the case of Nexus5 + YouTube app, it comes out in front of the navigation bar.
 */
internal class FloatingView
/**
 * Constructor
 *
 * @param context [android.content.Context]
 */
    (context: Context) : FrameLayout(context), ViewTreeObserver.OnPreDrawListener {

    /**
     * WindowManager
     */
    private  val mWindowManager :  WindowManager

    /**
     * LayoutParams
     */
    /**
     * Get WindowManager.LayoutParams.
     */
    val windowLayoutParams :  WindowManager . LayoutParams

    /**
     * VelocityTracker
     */
    private var mVelocityTracker: VelocityTracker? = null

    /**
     * [ViewConfiguration]
     */
    private var mViewConfiguration: ViewConfiguration? = null

    /**
     * Minimum threshold required for movement(px)
     */
    private var mMoveThreshold: Float = 0.toFloat()

    /**
     * Maximum fling velocity
     */
    private var mMaximumFlingVelocity: Float = 0.toFloat()

    /**
     * Maximum x coordinate velocity
     */
    private var mMaximumXVelocity: Float = 0.toFloat()

    /**
     * Maximum x coordinate velocity
     */
    private var mMaximumYVelocity: Float = 0.toFloat()

    /**
     * Threshold to move when throwing
     */
    private var mThrowMoveThreshold: Float = 0.toFloat()

    /**
     * DisplayMetrics
     */
    private val mMetrics: DisplayMetrics

    /**
     * Time to check if the press process has passed
     */
    private var mTouchDownTime: Long = 0

    /**
     * Screen press X coordinate (for determining movement amount)
     */
    private var mScreenTouchDownX: Float = 0.toFloat()
    /**
     * Screen press Y coordinate (for determining movement amount)
     */
    private var mScreenTouchDownY: Float = 0.toFloat()
    /**
     * Flag that started moving once
     */
    private var mIsMoveAccept: Boolean = false

    /**
     * Screen touch X coordinates
     */
    private var mScreenTouchX: Float = 0.toFloat()
    /**
     * Screen touch Y coordinates
     */
    private var mScreenTouchY: Float = 0.toFloat()
    /**
     * Local touch X coordinates
     */
    private var mLocalTouchX: Float = 0.toFloat()
    /**
     * Local touch Y coordinate
     */
    private var mLocalTouchY: Float = 0.toFloat()

    /**
     * Initial display X coordinate
     */
    private var mInitX: Int = 0
    /**
     * Initial display Y coordinate
     */
    private var mInitY: Int = 0

    /**
     * Initial animation running flag
     */
    private var mIsInitialAnimationRunning: Boolean = false

    /**
     * Flags that animate during initial display
     */
    private var mAnimateInitialMove: Boolean = false

    /**
     * status bar's height
     */
    private val mBaseStatusBarHeight: Int

    /**
     * status bar's height(landscape)
     */
    private val mBaseStatusBarRotatedHeight: Int

    /**
     * Current status bar's height
     */
    private  var mStatusBarHeight :  Int  =  0

    /**
     * Navigation bar's height(portrait)
     */
    private val mBaseNavigationBarHeight: Int

    /**
     * Navigation bar's height
     * Placed bottom on the screen(tablet)
     * Or placed vertically on the screen(phone)
     */
    private val mBaseNavigationBarRotatedHeight: Int

    /**
     * Current Navigation bar's vertical size
     */
    private var mNavigationBarVerticalOffset: Int = 0

    /**
     * Current Navigation bar's horizontal size
     */
    private var mNavigationBarHorizontalOffset: Int = 0

    /**
     * Offset of touch X coordinate
     */
    private var mTouchXOffset: Int = 0

    /**
     * Offset of touch Y coordinate
     */
    private var mTouchYOffset: Int = 0

    /**
     * Animation to move to the left and right edges
     */
    private var mMoveEdgeAnimator: ValueAnimator? = null

    /**
     * Interpolator
     */
    private val mMoveEdgeInterpolator: TimeInterpolator

    /**
     * Rect representing the movement limit
     */
    private val mMoveLimitRect: Rect

    /**
     * Rect that represents the limit of the display position (screen edge)
     */
    private val mPositionLimitRect: Rect

    /**
     * Draggable flag
     */
    private  var mIsDraggable :  Boolean  =  false

    /**
     * Factor representing the shape
     */
    /**
     * Gets the shape of the View.
     *
     * @return SHAPE_CIRCLE or SHAPE_RECTANGLE
     */
    /**
     * A constant that represents the shape of the View
     *
     * @param shape SHAPE_CIRCLE or SHAPE_RECTANGLE
     */
    var shape: Float = 0F

    /**
     * Handler to animate FloatingView
     */
    private val mAnimationHandler: FloatingAnimationHandler

    /**
     * Handler to judge long press
     */
    private val mLongPressHandler: LongPressHandler

    /**
     * Margin over the edge of the screen
     */
    private var mOverMargin: Int = 0

    /**
     * OnTouchListener
     */
    private var mOnTouchListener: OnTouchListener? = null

    /**
     * In the case of long press state
     */
    private var mIsLongPressed: Boolean = false

    /**
     * Direction of movement
     */
    private var mMoveDirection: Int = 0

    /**
     * Use dynamic physics-based animations or not
     */
    private var mUsePhysics: Boolean = false

    /**
     * If true, it's a tablet. If false, it's a phone
     */
    private val mIsTablet: Boolean

    /**
     * Surface.ROTATION_XXX
     */
    private var mRotation: Int = 0

    /**
     * Cutout safe inset rect(Same as FloatingViewManager's mSafeInsetRect)
     */
    private val mSafeInsetRect: Rect

    private var realTimePositionListener: RealTimePositionListener? = null

    /**
     * FloatingView X coordinates calculated from touch coordinates
     *
     * @return FloatingView X coordinate
     */
    private val xByTouch: Int
        get() = (mScreenTouchX - mLocalTouchX - mTouchXOffset.toFloat()).toInt()

    /**
     * Y coordinate of FloatingView calculated from touch coordinates
     *
     * @return FloatingView Y coordinate
     */
    private val yByTouch: Int
        get() = (mMetrics.heightPixels + mNavigationBarVerticalOffset - (mScreenTouchY - mLocalTouchY + height - mTouchYOffset)).toInt()

    val state: Int
        get() = mAnimationHandler.state

    val heightLimit: Int
        get() = mPositionLimitRect.height()

    /**
     * AnimationState
     */
    @IntDef(STATE_NORMAL, STATE_INTERSECTING, STATE_FINISHING)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    internal annotation class AnimationState

    init {
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowLayoutParams = WindowManager.LayoutParams()
        mMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        mWindowManager.defaultDisplay.getMetrics(mMetrics)
        windowLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        windowLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        windowLayoutParams.type = OVERLAY_TYPE
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        windowLayoutParams.format = PixelFormat.TRANSLUCENT
        /// Set the lower left coordinate to 0
        windowLayoutParams.gravity = Gravity.START or Gravity.BOTTOM
        mAnimationHandler = FloatingAnimationHandler(this)
        mLongPressHandler = LongPressHandler(this)
        mMoveEdgeInterpolator = OvershootInterpolator(MOVE_TO_EDGE_OVERSHOOT_TENSION)
        mMoveDirection = FloatingViewManager.MOVE_DIRECTION_DEFAULT
        mUsePhysics = false
        val resources = context.resources
        mIsTablet =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        @Suppress("DEPRECATION")
        mRotation = mWindowManager.defaultDisplay.rotation

        mMoveLimitRect = Rect()
        mPositionLimitRect = Rect()
        mSafeInsetRect = Rect()

        // Get the height of the status bar
        mBaseStatusBarHeight = getSystemUiDimensionPixelSize(resources, "status_bar_height")
        // Check landscape resource id
        val statusBarLandscapeResId =
            resources.getIdentifier("status_bar_height_landscape", "dimen", "android")
        mBaseStatusBarRotatedHeight = if (statusBarLandscapeResId > 0) {
            getSystemUiDimensionPixelSize(resources, "status_bar_height_landscape")
        } else {
            mBaseStatusBarHeight
        }

        // Init physics-based animation properties
        updateViewConfiguration()

        // Detect NavigationBar
        if (hasSoftNavigationBar()) {
            mBaseNavigationBarHeight =
                getSystemUiDimensionPixelSize(resources, "navigation_bar_height")
            val resName =
                if (mIsTablet) "navigation_bar_height_landscape" else "navigation_bar_width"
            mBaseNavigationBarRotatedHeight = getSystemUiDimensionPixelSize(resources, resName)
        } else {
            mBaseNavigationBarHeight = 0
            mBaseNavigationBarRotatedHeight = 0
        }

        // For initial drawing process
        viewTreeObserver.addOnPreDrawListener(this)
    }

    /**
     * Check if there is a software navigation bar(including the navigation bar in the screen).
     *
     * @return True if there is a software navigation bar
     */
    private fun hasSoftNavigationBar(): Boolean {
        val realDisplayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        mWindowManager.defaultDisplay.getRealMetrics(realDisplayMetrics)
        return realDisplayMetrics.heightPixels > mMetrics.heightPixels || realDisplayMetrics.widthPixels > mMetrics.widthPixels

        // old device check flow
        // Navigation bar exists (config_showNavigationBar is true, or both the menu key and the back key are not exists)
    }


    /**
     * Determine the display position.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        refreshLimitRect()
    }

    /**
     * Adjust the layout when the screen is rotated.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateViewConfiguration()
        refreshLimitRect()
    }

    /**
     * Set the coordinates for the first drawing.
     */
    override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)
        // Enter the default value if the initial value is set in the X coordinate (margin is not considered)
        if (mInitX == DEFAULT_X) {
            mInitX =  0
        }
        // Enter the default value if the initial value is set for the Y coordinate
        if (mInitY == DEFAULT_Y) {
            mInitY = mMetrics.heightPixels - mStatusBarHeight - measuredHeight
        }

        // Set the initial position
        windowLayoutParams.x = mInitX
        windowLayoutParams.y = mInitY

        // If it does not move to the edge of the screen, move to the specified coordinates
        if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_NONE) {
            moveTo(mInitX, mInitY, mInitX, mInitY, false)
        } else {
            mIsInitialAnimationRunning = true
            // Move from the initial position to the edge of the screen
            moveToEdge(mInitX, mInitY, mAnimateInitialMove)
        }
        mIsDraggable =  true
        updateViewLayout()
        return true
    }

    /**
     * Called when the layout of the system has changed.
     *
     * @param isHideStatusBar     If true, the status bar is hidden
     * @param isHideNavigationBar If true, the navigation bar is hidden
     * @param isPortrait          If true, the device orientation is portrait
     * @param windowRect          [Rect] of system window
     */
    fun onUpdateSystemLayout(
        isHideStatusBar: Boolean,
        isHideNavigationBar: Boolean,
        isPortrait: Boolean,
        windowRect: Rect
    ) {
        // status bar
        updateStatusBarHeight(isHideStatusBar, isPortrait)
        // touch X offset(support Cutout)
        updateTouchXOffset(isHideNavigationBar, windowRect.left)
        // touch Y offset(support Cutout)
        mTouchYOffset = if (isPortrait) mSafeInsetRect.top else 0
        // navigation bar
        updateNavigationBarOffset(isHideNavigationBar, isPortrait, windowRect)
        refreshLimitRect()
    }

    /**
     * Update height of StatusBar.
     *
     * @param isHideStatusBar If true, the status bar is hidden
     * @param isPortrait      If true, the device orientation is portrait
     */
    private fun updateStatusBarHeight(isHideStatusBar: Boolean, isPortrait: Boolean) {
        if (isHideStatusBar) {
            // 1.(No Cutout)No StatusBar(=0)
            // 2.(Has Cutout)StatusBar is not included in mMetrics.heightPixels (=0)
            mStatusBarHeight = 0
            return
        }

        // Has Cutout
        val hasTopCutout = mSafeInsetRect.top != 0
        if (hasTopCutout) {
            mStatusBarHeight = if (isPortrait) {
                0
            } else {
                mBaseStatusBarRotatedHeight
            }
            return
        }

        // No cutout
        mStatusBarHeight = if (isPortrait) {
            mBaseStatusBarHeight
        } else {
            mBaseStatusBarRotatedHeight
        }
    }

    /**
     * Update of touch X coordinate
     *
     * @param isHideNavigationBar If true, the navigation bar is hidden
     * @param windowLeftOffset    Left side offset of device display
     */
    private fun updateTouchXOffset(isHideNavigationBar: Boolean, windowLeftOffset: Int) {
        val hasBottomCutout = mSafeInsetRect.bottom != 0
        if (hasBottomCutout) {
            mTouchXOffset = windowLeftOffset
            return
        }

        // No cutout
        // touch X offset(navigation bar is displayed and it is on the left side of the device)
        mTouchXOffset =
            if (!isHideNavigationBar && windowLeftOffset > 0) mBaseNavigationBarRotatedHeight else 0
    }

    /**
     * Update offset of NavigationBar.
     *
     * @param isHideNavigationBar If true, the navigation bar is hidden
     * @param isPortrait          If true, the device orientation is portrait
     * @param windowRect          [Rect] of system window
     */
    private fun updateNavigationBarOffset(
        isHideNavigationBar: Boolean,
        isPortrait: Boolean,
        windowRect: Rect
    ) {
        val currentNavigationBarHeight: Int
        val currentNavigationBarWidth: Int
        val navigationBarVerticalDiff: Int
        val hasSoftNavigationBar = hasSoftNavigationBar()
        // auto hide navigation bar(Galaxy S8, S9 and so on.)
        val realDisplayMetrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getRealMetrics(realDisplayMetrics)
        currentNavigationBarHeight = realDisplayMetrics.heightPixels - windowRect.bottom
        currentNavigationBarWidth = realDisplayMetrics.widthPixels - mMetrics.widthPixels
        navigationBarVerticalDiff = mBaseNavigationBarHeight - currentNavigationBarHeight

        if (!isHideNavigationBar) {
            // auto hide navigation bar
            // Guess based on inconsistency with other devices
            // 1. The height of the navigation bar (mBaseNavigationBarHeight == 0) built into the device does not differ depending on the system status.
            // 2. The navigation bar (! HasSoftNavigationBar) built into the device is inconsistent because it intentionally sets Base to 0.
            mNavigationBarVerticalOffset = if (navigationBarVerticalDiff != 0
                && mBaseNavigationBarHeight == 0 || !hasSoftNavigationBar
                && mBaseNavigationBarHeight != 0
            ) {
                if (hasSoftNavigationBar) {
                    // 1.auto hide mode -> show mode
                    // 2.show mode -> auto hide mode -> home
                    0
                } else {
                    // show mode -> home
                    -currentNavigationBarHeight
                }
            } else {
                // normal device
                0
            }

            mNavigationBarHorizontalOffset = 0
            return
        }

        // If the portrait, is displayed at the bottom of the screen
        if (isPortrait) {
            // auto hide navigation bar
            mNavigationBarVerticalOffset =
                if (!hasSoftNavigationBar && mBaseNavigationBarHeight != 0) {
                    0
                } else {
                    mBaseNavigationBarHeight
                }
            mNavigationBarHorizontalOffset = 0
            return
        }

        // If it is a Tablet, it will appear at the bottom of the screen.
        // If it is Phone, it will appear on the side of the screen
        if (mIsTablet) {
            mNavigationBarVerticalOffset = mBaseNavigationBarRotatedHeight
            mNavigationBarHorizontalOffset = 0
        } else {
            mNavigationBarVerticalOffset = 0
            // auto hide navigation bar
            // Guess based on inconsistency with other devices
            // 1. The navigation bar (! HasSoftNavigationBar) built into the device is inconsistent because Base is intentionally set to 0.
            mNavigationBarHorizontalOffset =
                if (!hasSoftNavigationBar && mBaseNavigationBarRotatedHeight != 0) {
                    0
                } else if (hasSoftNavigationBar && mBaseNavigationBarRotatedHeight == 0) {
                    // 2. In case of soft navigation bar, it is inconsistent because Base is set.
                    currentNavigationBarWidth
                } else {
                    mBaseNavigationBarRotatedHeight
                }
        }
    }

    /**
     * Update [ViewConfiguration]
     */
    private fun updateViewConfiguration() {
        mViewConfiguration = ViewConfiguration.get(context)
        mMoveThreshold = mViewConfiguration!!.scaledTouchSlop.toFloat()
        mMaximumFlingVelocity = mViewConfiguration!!.scaledMaximumFlingVelocity.toFloat()
        mMaximumXVelocity = mMaximumFlingVelocity / MAX_X_VELOCITY_SCALE_DOWN_VALUE
        mMaximumYVelocity = mMaximumFlingVelocity / MAX_Y_VELOCITY_SCALE_DOWN_VALUE
        mThrowMoveThreshold = mMaximumFlingVelocity / THROW_THRESHOLD_SCALE_DOWN_VALUE
    }

    /**
     * Update the PositionLimitRect and MoveLimitRect according to the screen size change.
     */
    private fun refreshLimitRect() {
        cancelAnimation()

        // Save previous screen coordinates
        val oldPositionLimitWidth = mPositionLimitRect.width()
        val oldPositionLimitHeight = mPositionLimitRect.height()

        // Switch to new coordinate information
        mWindowManager.defaultDisplay.getMetrics(mMetrics)
        val width = measuredWidth
        val height = measuredHeight
        val newScreenWidth = mMetrics.widthPixels
        val newScreenHeight = mMetrics.heightPixels

        // Setting the movement range
        mMoveLimitRect.set(
            -width,
            -height * 2,
            newScreenWidth + width + mNavigationBarHorizontalOffset,
            newScreenHeight + height + mNavigationBarVerticalOffset
        )
        mPositionLimitRect.set(
            -mOverMargin,
            0,
            newScreenWidth - width + mOverMargin + mNavigationBarHorizontalOffset,
            newScreenHeight - mStatusBarHeight - height + mNavigationBarVerticalOffset
        )

        // Initial animation stop when the device rotates
        val newRotation = mWindowManager.defaultDisplay.rotation
        if (mAnimateInitialMove && mRotation != newRotation) {
            mIsInitialAnimationRunning = false
        }

        // When animation is running and the device is not rotating
        if (mIsInitialAnimationRunning && mRotation == newRotation) {
            moveToEdge(windowLayoutParams.x, windowLayoutParams.y, true)
        } else {
            // If there is a screen change during the operation, move to the appropriate position
            if (mIsMoveAccept) {
                moveToEdge(windowLayoutParams.x, windowLayoutParams.y, false)
            } else {
                val newX =
                    (windowLayoutParams.x * mPositionLimitRect.width() / oldPositionLimitWidth.toFloat() + 0.5f).toInt()
                val goalPositionX =
                    min(max(mPositionLimitRect.left, newX), mPositionLimitRect.right)
                val newY =
                    (windowLayoutParams.y * mPositionLimitRect.height() / oldPositionLimitHeight.toFloat() + 0.5f).toInt()
                val goalPositionY =
                    min(max(mPositionLimitRect.top, newY), mPositionLimitRect.bottom)
                moveTo(
                    windowLayoutParams.x,
                    windowLayoutParams.y,
                    goalPositionX,
                    goalPositionY,
                    false
                )
            }
        }
        mRotation = newRotation
    }

    /**
     * {@inheritDoc}
     */
    override fun onDetachedFromWindow() {
        if (mMoveEdgeAnimator != null) {
            mMoveEdgeAnimator!!.removeAllUpdateListeners()
        }
        super.onDetachedFromWindow()
    }

    /**
     * {@inheritDoc}
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Do nothing if View is not displayed
        if (visibility != View.VISIBLE) {
            return true
        }

        // Do nothing if touch is not possible
        if (!mIsDraggable) {
            return true
        }

        // Block while initial display animation is running
        if (mIsInitialAnimationRunning) {
            return true
        }

        // Current location cache
        mScreenTouchX = event.rawX
        mScreenTouchY = event.rawY
        val action = event.action
        var isWaitForMoveToEdge = false
        // Press
        if (action == MotionEvent.ACTION_DOWN) {
            // Cancel animation
            cancelAnimation()
            mScreenTouchDownX = mScreenTouchX
            mScreenTouchDownY = mScreenTouchY
            mLocalTouchX = event.x
            mLocalTouchY = event.y
            mIsMoveAccept = false
            setScale(SCALE_PRESSED)

            if (mVelocityTracker == null) {
                // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                mVelocityTracker = VelocityTracker.obtain()
            } else {
                // Reset the velocity tracker back to its initial state.
                mVelocityTracker!!.clear()
            }

            // Start touch tracking animation
            mAnimationHandler.updateTouchPosition(xByTouch.toFloat(), yByTouch.toFloat())
            mAnimationHandler.removeMessages(FloatingAnimationHandler.ANIMATION_IN_TOUCH)
            mAnimationHandler.sendAnimationMessage(FloatingAnimationHandler.ANIMATION_IN_TOUCH)
            // Start long press judgment
            mLongPressHandler.removeMessages(LongPressHandler.LONG_PRESSED)
            mLongPressHandler.sendEmptyMessageDelayed(
                LongPressHandler.LONG_PRESSED,
                LONG_PRESS_TIMEOUT.toLong()
            )
            // Hold time to judge the passage of the press process
            // To prevent MOVE etc. from being processed when the mIsDraggable or getVisibility () flags are changed after pressing
            mTouchDownTime = event.downTime
            // compute offset and restore
            addMovement(event)
            mIsInitialAnimationRunning = false
        } else if (action == MotionEvent.ACTION_MOVE) {
            // compute offset and restore
            if (mIsMoveAccept) {
                mIsLongPressed = false
                mLongPressHandler.removeMessages(LongPressHandler.LONG_PRESSED)
            }
            // Release long press in case of movement judgment
            if (mTouchDownTime != event.downTime) {
                return true
            }
            // Release long press in case of movement judgment
            if (!mIsMoveAccept && abs(mScreenTouchX - mScreenTouchDownX) < mMoveThreshold && abs(
                    mScreenTouchY - mScreenTouchDownY
                ) < mMoveThreshold
            ) {
                return true
            }
            mIsMoveAccept = true
            mAnimationHandler.updateTouchPosition(xByTouch.toFloat(), yByTouch.toFloat())
            // compute offset and restore
            addMovement(event)
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // compute velocity tracker
            if (mVelocityTracker != null) {
                mVelocityTracker!!.computeCurrentVelocity(CURRENT_VELOCITY_UNITS)
            }

            // Temporarily hold the long press state for judgment
            val tmpIsLongPressed = mIsLongPressed
            // Release long press
            mIsLongPressed = false
            mLongPressHandler.removeMessages(LongPressHandler.LONG_PRESSED)
            // Do not process if pressing process is not performed
            if (mTouchDownTime != event.downTime) {
                return true
            }
            // Delete animation
            mAnimationHandler.removeMessages(FloatingAnimationHandler.ANIMATION_IN_TOUCH)
            // Restore the magnification
            setScale(SCALE_NORMAL)

            // destroy VelocityTracker (#103)
            if (!mIsMoveAccept && mVelocityTracker != null) {
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
            }

            // When ACTION_UP is done (when not pressed or moved)
            if (action == MotionEvent.ACTION_UP && !tmpIsLongPressed && !mIsMoveAccept) {
                val size = childCount
                for (i in 0 until size) {
                    getChildAt(i).performClick()
                }
            } else {
                // Make a move after checking whether it is finished or not
                isWaitForMoveToEdge = true
            }
        }// Push up, cancel
        // Move

        // Notify touch listener
        if (mOnTouchListener != null) {
            mOnTouchListener!!.onTouch(this, event)
        }

        // Lazy execution of moveToEdge
        if (isWaitForMoveToEdge && mAnimationHandler.state != STATE_FINISHING) {
            // include device rotation
            moveToEdge(true)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
            }
        }

        return true
    }

    /**
     * Call addMovement and restore MotionEvent coordinate
     *
     * @param event [MotionEvent]
     */
    private fun addMovement(event: MotionEvent) {
        val deltaX = event.rawX - event.x
        val deltaY = event.rawY - event.y
        event.offsetLocation(deltaX, deltaY)
        mVelocityTracker!!.addMovement(event)
        event.offsetLocation(-deltaX, -deltaY)
    }

    /**
     * This is the process when the button is pressed and held.
     */
    private fun onLongClick() {
        mIsLongPressed = true
        // Long press processing
        val size = childCount
        for (i in 0 until size) {
            getChildAt(i).performLongClick()
        }
    }

    /**
     * Indicates the process for removing from the screen.
     */
    override fun setVisibility(visibility: Int) {
        //When the screen is displayed
        if (visibility != View.VISIBLE) {
            // To remove from the screen, cancel the long press and forcibly move to the edge of the screen.
            cancelLongPress()
            setScale(SCALE_NORMAL)
            if (mIsMoveAccept) {
                moveToEdge(false)
            }
            mAnimationHandler.removeMessages(FloatingAnimationHandler.ANIMATION_IN_TOUCH)
            mLongPressHandler.removeMessages(LongPressHandler.LONG_PRESSED)
        }
        super.setVisibility(visibility)
    }

    /**
     * {@inheritDoc}
     */
    override fun setOnTouchListener(listener: OnTouchListener) {
        mOnTouchListener = listener
    }

    /**
     * Move to the left and right edges.
     *
     * @param withAnimation true if you want to animate, false if you don't
     */
    private fun moveToEdge(withAnimation: Boolean) {
        val currentX = xByTouch
        val currentY = yByTouch
        moveToEdge(currentX, currentY, withAnimation)
    }

    /**
     * Specify the start point and move to the left and right edges.
     *
     * @param startX Initial value of X coordinate
     * @param startY Initial value of Y coordinate
     * @param withAnimation true if you want to animate, false if you don't
     */
    private fun moveToEdge(startX: Int, startY: Int, withAnimation: Boolean) {
        // Move to specified coordinates
        val goalPositionX = getGoalPositionX(startX, startY)
        val goalPositionY = getGoalPositionY(startX, startY)
        moveTo(startX, startY, goalPositionX, goalPositionY, withAnimation)
    }

    /**
     * Moves to the specified coordinates. <br> </br>
     * If the coordinates of the screen edge are exceeded, it will automatically move to the screen edge.
     *
     * @param currentX Current X coordinate (used for the start of the animation)
     * @param currentY Current Y coordinate (used for the start of the animation)
     * @param goalPositionX X coordinate of destination
     * @param goalPositionY Y coordinate of destination
     * @param withAnimation true if you want to animate, false if you don't
     */
    private fun moveTo(
        currentX: Int,
        currentY: Int,
        goalPositionX: Int,
        goalPositionY: Int,
        withAnimation: Boolean
    ) {
        var goalPositionX1 = goalPositionX
        var goalPositionY1 = goalPositionY
        // Adjust so that it does not protrude from the edge of the screen
        goalPositionX1 = min(max(mPositionLimitRect.left, goalPositionX1), mPositionLimitRect.right)
        goalPositionY1 = min(max(mPositionLimitRect.top, goalPositionY1), mPositionLimitRect.bottom)
        // When animating
        if (withAnimation) {
            // Use physics animation
            val usePhysicsAnimation =
                mUsePhysics && mVelocityTracker != null && mMoveDirection != FloatingViewManager.MOVE_DIRECTION_NEAREST
            if (usePhysicsAnimation) {
                startPhysicsAnimation(goalPositionX1, currentY)
            } else {
                startObjectAnimation(currentX, currentY, goalPositionX1, goalPositionY1)
            }
        } else {
            // Update only when the position changes
            if (windowLayoutParams.x != goalPositionX1 || windowLayoutParams.y != goalPositionY1) {
                windowLayoutParams.x = goalPositionX1
                windowLayoutParams.y = goalPositionY1
                updateViewLayout()
            }
        }
        // Initialize touch coordinates
        mLocalTouchX = 0f
        mLocalTouchY = 0f
        mScreenTouchDownX = 0f
        mScreenTouchDownY = 0f
        mIsMoveAccept = false
    }

    /**
     * Start Physics-based animation
     *
     * @param goalPositionX goal position X coordinate
     * @param currentY      current Y coordinate
     */
    private fun startPhysicsAnimation(goalPositionX: Int, currentY: Int) {
        // start X coordinate animation
        val containsLimitRectWidth =
            windowLayoutParams.x < mPositionLimitRect.right && windowLayoutParams.x > mPositionLimitRect.left
        // If MOVE_DIRECTION_NONE, play fling animation
        if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_NONE && containsLimitRectWidth) {
            val velocityX =
                min(max(mVelocityTracker!!.xVelocity, -mMaximumXVelocity), mMaximumXVelocity)
            startFlingAnimationX(velocityX)
        } else {
            startSpringAnimationX(goalPositionX)
        }

        // start Y coordinate animation
        val containsLimitRectHeight =
            windowLayoutParams.y < mPositionLimitRect.bottom && windowLayoutParams.y > mPositionLimitRect.top
        val velocityY =
            -min(max(mVelocityTracker!!.yVelocity, -mMaximumYVelocity), mMaximumYVelocity)
        if (containsLimitRectHeight) {
            startFlingAnimationY(velocityY)
        } else {
            startSpringAnimationY(currentY, velocityY)
        }
    }

    /**
     * Start object animation
     *
     * @param currentX      current X coordinate
     * @param currentY      current Y coordinate
     * @param goalPositionX goal position X coordinate
     * @param goalPositionY goal position Y coordinate
     */
    private fun startObjectAnimation(
        currentX: Int,
        currentY: Int,
        goalPositionX: Int,
        goalPositionY: Int
    ) {
        if (goalPositionX == currentX) {
            //to move only y coord
            mMoveEdgeAnimator = ValueAnimator.ofInt(currentY, goalPositionY)
            mMoveEdgeAnimator!!.addUpdateListener { animation ->
                windowLayoutParams.y = animation.animatedValue as Int
                updateViewLayout()
                updateInitAnimation(animation)
            }
        } else {
            // To move only x coord (to left or right)
            windowLayoutParams.y = goalPositionY
            mMoveEdgeAnimator = ValueAnimator.ofInt(currentX, goalPositionX)
            mMoveEdgeAnimator!!.addUpdateListener { animation ->
                windowLayoutParams.x = animation.animatedValue as Int
                updateViewLayout()
                updateInitAnimation(animation)
            }
        }
        // X-axis animation settings
        mMoveEdgeAnimator!!.duration = MOVE_TO_EDGE_DURATION
        mMoveEdgeAnimator!!.interpolator = mMoveEdgeInterpolator
        mMoveEdgeAnimator!!.start()
    }

    /**
     * Start spring animation(X coordinate)
     *
     * @param goalPositionX goal position X coordinate
     */
    private fun startSpringAnimationX(goalPositionX: Int) {
        // springX
        val springX = SpringForce(goalPositionX.toFloat())
        springX.dampingRatio = ANIMATION_SPRING_X_DAMPING_RATIO
        springX.stiffness = ANIMATION_SPRING_X_STIFFNESS
        // springAnimation
        SpringAnimation(FloatValueHolder()).apply {
            setStartVelocity(mVelocityTracker!!.xVelocity)
            setStartValue(windowLayoutParams.x.toFloat())
            spring = springX
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
            addUpdateListener(DynamicAnimation.OnAnimationUpdateListener { _, value, _ ->
                val x = value.roundToInt()
                // Not moving, or the touch operation is continuing
                if (windowLayoutParams.x == x || mVelocityTracker != null) {
                    return@OnAnimationUpdateListener
                }
                // update x coordinate
                windowLayoutParams.x = x
                updateViewLayout()
            })
            start()
        }
    }

    /**
     * Start spring animation(Y coordinate)
     *
     * @param currentY  current Y coordinate
     * @param velocityY velocity Y coordinate
     */
    private fun startSpringAnimationY(currentY: Int, velocityY: Float) {
        // Create SpringForce
        val springY =
            SpringForce((if (currentY < mMetrics.heightPixels / 2) mPositionLimitRect.top else mPositionLimitRect.bottom).toFloat())
        springY.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        springY.stiffness = SpringForce.STIFFNESS_LOW

        // Create SpringAnimation
        SpringAnimation(FloatValueHolder()).apply {
            setStartVelocity(velocityY)
            setStartValue(windowLayoutParams.y.toFloat())
            spring = springY
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
            addUpdateListener(DynamicAnimation.OnAnimationUpdateListener { _, value, _ ->
                val y = value.roundToInt()
                // Not moving, or the touch operation is continuing
                if (windowLayoutParams.y == y || mVelocityTracker != null) {
                    return@OnAnimationUpdateListener
                }
                // update y coordinate
                windowLayoutParams.y = y
                updateViewLayout()
            })
            start()
        }
    }

    /**
     * Start fling animation(X coordinate)
     *
     * @param velocityX velocity X coordinate
     */
    private fun startFlingAnimationX(velocityX: Float) {
        FlingAnimation(FloatValueHolder()).apply {
            setStartVelocity(velocityX)
            setMaxValue(mPositionLimitRect.right.toFloat())
            setMinValue(mPositionLimitRect.left.toFloat())
            setStartValue(windowLayoutParams.x.toFloat())
            friction = ANIMATION_FLING_X_FRICTION
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
            addUpdateListener(DynamicAnimation.OnAnimationUpdateListener { _, value, _ ->
                val x = value.roundToInt()
                // Not moving, or the touch operation is continuing
                if (windowLayoutParams.x == x || mVelocityTracker != null) {
                    return@OnAnimationUpdateListener
                }
                // update y coordinate
                windowLayoutParams.x = x
                updateViewLayout()
            })
            start()
        }
    }

    /**
     * Start fling animation(Y coordinate)
     *
     * @param velocityY velocity Y coordinate
     */
    private fun startFlingAnimationY(velocityY: Float) {
        FlingAnimation(FloatValueHolder()).apply {
            setStartVelocity(velocityY)
            setMaxValue(mPositionLimitRect.bottom.toFloat())
            setMinValue(mPositionLimitRect.top.toFloat())
            setStartValue(windowLayoutParams.y.toFloat())
            friction = ANIMATION_FLING_Y_FRICTION
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
            addUpdateListener(DynamicAnimation.OnAnimationUpdateListener { _, value, _ ->
                val y = value.roundToInt()
                // Not moving, or the touch operation is continuing
                if (windowLayoutParams.y == y || mVelocityTracker != null) {
                    return@OnAnimationUpdateListener
                }
                // update y coordinate
                windowLayoutParams.y = y
                updateViewLayout()
            })
            start()
        }
    }

    /**
     * Check if it is attached to the Window and call WindowManager.updateLayout()
     */
    private fun updateViewLayout() {
        if (!ViewCompat.isAttachedToWindow(this)) {
            return
        }
        mWindowManager.updateViewLayout(this, windowLayoutParams)
        realTimePositionListener!!.currentPosition()
    }

    /**
     * Update animation initialization flag
     *
     * @param animation [ValueAnimator]
     */
    private fun updateInitAnimation(animation: ValueAnimator) {
        if (mAnimateInitialMove && animation.duration <= animation.currentPlayTime) {
            mIsInitialAnimationRunning = false
        }
    }

    /**
     * Get the final point of movement (X coordinate)
     *
     * @param startX Initial value of X coordinate
     * @param startY Initial value of Y coordinate
     * @return End point of X coordinate
     */
    private fun getGoalPositionX(startX: Int, startY: Int): Int {
        var goalPositionX = startX

        // Move to left or right edges
        if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_DEFAULT) {
            val isMoveRightEdge = startX > (mMetrics.widthPixels - width) / 2
            goalPositionX =
                if (isMoveRightEdge) mPositionLimitRect.right else mPositionLimitRect.left
        } else if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_LEFT) {
            goalPositionX = mPositionLimitRect.left
        } else if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_RIGHT) {
            goalPositionX = mPositionLimitRect.right
        } else if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_NEAREST) {
            val distLeftRight = min(startX, mPositionLimitRect.width() - startX)
            val distTopBottom = min(startY, mPositionLimitRect.height() - startY)
            if (distLeftRight < distTopBottom) {
                val isMoveRightEdge = startX > (mMetrics.widthPixels - width) / 2
                goalPositionX =
                    if (isMoveRightEdge) mPositionLimitRect.right else mPositionLimitRect.left
            }
        } else if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_THROWN) {
            goalPositionX =
                if (mVelocityTracker != null && mVelocityTracker!!.xVelocity > mThrowMoveThreshold) {
                    mPositionLimitRect.right
                } else if (mVelocityTracker != null && mVelocityTracker!!.xVelocity < -mThrowMoveThreshold) {
                    mPositionLimitRect.left
                } else {
                    val isMoveRightEdge = startX > (mMetrics.widthPixels - width) / 2
                    if (isMoveRightEdge) mPositionLimitRect.right else mPositionLimitRect.left
                }
        }// Move in the direction in which it is thrown
        // Move to top/bottom/left/right edges
        // Move to right edges
        // Move to left edges

        return goalPositionX
    }

    /**
     * Get the final point of movement (Y coordinate)
     *
     * @param startX Initial value of X coordinate
     * @param startY Initial value of Y coordinate
     * @return End point of Y coordinate
     */
    private fun getGoalPositionY(startX: Int, startY: Int): Int {
        var goalPositionY = startY

        // Move to top/bottom/left/right edges
        if (mMoveDirection == FloatingViewManager.MOVE_DIRECTION_NEAREST) {
            val distLeftRight = min(startX, mPositionLimitRect.width() - startX)
            val distTopBottom = min(startY, mPositionLimitRect.height() - startY)
            if (distLeftRight >= distTopBottom) {
                val isMoveTopEdge = startY < (mMetrics.heightPixels - height) / 2
                goalPositionY =
                    if (isMoveTopEdge) mPositionLimitRect.top else mPositionLimitRect.bottom
            }
        }

        return goalPositionY
    }

    /**
     * Cancel the animation.
     */
    private fun cancelAnimation() {
        if (mMoveEdgeAnimator != null && mMoveEdgeAnimator!!.isStarted) {
            mMoveEdgeAnimator!!.cancel()
            mMoveEdgeAnimator = null
        }
    }

    /**
     * Enlarge / reduce.
     *
     * @param newScale Scale to set
     */
    private fun setScale(newScale: Float) {
        // INFO:childにscaleを設定しないと拡大率が変わらない現象に対処するための修正
        scaleX = newScale
        scaleY = newScale
    }

    /**
     * Draggable flag
     *
     * @param isDraggable true if draggable
     */
    fun setDraggable(isDraggable: Boolean) {
        mIsDraggable = isDraggable
    }

    /**
     * Margin over the edge of the screen.
     *
     * @param margin Margin
     */
    fun setOverMargin(margin: Int) {
        mOverMargin = margin
    }

    /**
     * Set the movement direction.
     *
     * @param moveDirection move direction
     */
    fun setMoveDirection(moveDirection: Int) {
        mMoveDirection = moveDirection
    }

    /**
     * Use dynamic physics-based animations or not
     * Warning: Can not be used before API 16
     *
     * @param usePhysics Setting this to false will revert to using a ValueAnimator (default is true)
     */
    fun usePhysics(usePhysics: Boolean) {
        mUsePhysics = usePhysics
    }

    /**
     * Set the initial coordinates.
     *
     * @param x FloatingView initial X coordinates
     * @param y Initial Y coordinate of FloatingView
     */
    fun setInitCoords(x: Int, y: Int) {
        mInitX = x
        mInitY = y
    }

    /**
     * Set a flag to animate at initial display.
     *
     * @param animateInitialMove true to animate at initial display
     */
    fun setAnimateInitialMove(animateInitialMove: Boolean) {
        mAnimateInitialMove = animateInitialMove
    }

    /**
     * Gets the drawing area on the Window.
     *
     * @param outRect Make changes Rect
     */
    fun getWindowDrawingRect(outRect: Rect) {
        val currentX = xByTouch
        val currentY = yByTouch
        outRect.set(currentX, currentY, currentX + width, currentY + height)
    }

    /**
     * Change to normal state.
     */
    fun setNormal() {
        mAnimationHandler.state = STATE_NORMAL
        mAnimationHandler.updateTouchPosition(xByTouch.toFloat(), yByTouch.toFloat())
    }

    /**
     * Change to the overlapping state.
     *
     * @param centerX Target center coordinates X
     * @param centerY Target center coordinates Y
     */
    fun setIntersecting(centerX: Int, centerY: Int) {
        mAnimationHandler.state = STATE_INTERSECTING
        mAnimationHandler.updateTargetPosition(centerX.toFloat(), centerY.toFloat())
    }

    /**
     * Change to the finished state.
     */
    fun setFinishing() {
        mAnimationHandler.state = STATE_FINISHING
        mIsMoveAccept = false
        visibility = View.GONE
    }

    /**
     * Set the cutout's safe inset area
     *
     * @param safeInsetRect [FloatingViewManager.setSafeInsetRect]
     */
    fun setSafeInsetRect(safeInsetRect: Rect) {
        mSafeInsetRect.set(safeInsetRect)
    }

    /**
     * Set the cutout's safe inset area
     *
     * @param safeInsetRect [FloatingViewManager.setSafeInsetRect]
     */
    internal class FloatingAnimationHandler

    /**
     * A handler that controls animation.
     */
        (floatingView: FloatingView) : Handler() {


        /**
         * Time when the animation started
         */
        private var mStartTime: Long = 0

        /**
         * TransitionX at the beginning of the animation
         */
        private var mStartX: Float = 0.toFloat()

        /**
         * TransitionY at the beginning of the animation
         */
        private var mStartY: Float = 0.toFloat()

        /**
         * Running animation code
         */
        private var mStartedCode: Int = 0

        /**
         * Animation state flag
         */
        private var mState: Int = 0

        /**
         * Current state
         */
        private var mIsChangeState: Boolean = false

        /**
         * X coordinate of the tracking target
         */
        private var mTouchPositionX: Float = 0.toFloat()

        /**
         * Y coordinate of the tracking target
         */
        private var mTouchPositionY: Float = 0.toFloat()

        /**
         * X coordinate of the tracking target
         */
        private var mTargetPositionX: Float = 0.toFloat()

        /**
         * Y coordinate of the tracking target
         */
        private var mTargetPositionY: Float = 0.toFloat()

        /**
         * FloatingView
         */
        private val mFloatingView: WeakReference<FloatingView> = WeakReference(floatingView)

        /**
         * Returns the current state.
         *
         * @return STATE_NORMAL or STATE_INTERSECTING or STATE_FINISHING
         */
        /**
         * Set the animation state.
         *
         * @param newState STATE_NORMAL or STATE_INTERSECTING or STATE_FINISHING
         */
        // Change state only if state is different Change flag
        var state: Int
            get() = mState
            set(@AnimationState newState) {
                if (mState != newState) {
                    mIsChangeState = true
                }
                mState = newState
            }

        init {
            mStartedCode = ANIMATION_NONE
            mState = STATE_NORMAL
        }

        /**
         * Performs animation processing.
         */
        override fun handleMessage(msg: Message) {
            val floatingView = mFloatingView.get()
            if (floatingView == null) {
                removeMessages(ANIMATION_IN_TOUCH)
                return
            }

            val animationCode = msg.what
            val animationType = msg.arg1
            val params = floatingView.windowLayoutParams

            // Initialization when state change or animation is started
            if (mIsChangeState || animationType == TYPE_FIRST) {
                // Use animation time only when changing state
                mStartTime = if (mIsChangeState) SystemClock.uptimeMillis() else 0
                mStartX = params.x.toFloat()
                mStartY = params.y.toFloat()
                mStartedCode = animationCode
                mIsChangeState = false
            }
            // elapsed time
            val elapsedTime = (SystemClock.uptimeMillis() - mStartTime).toFloat()
            val trackingTargetTimeRate = min(elapsedTime / CAPTURE_DURATION_MILLIS, 1.0f)

            // Animation when they do not overlap
            if (mState == FloatingView.STATE_NORMAL) {
                val basePosition = calcAnimationPosition(trackingTargetTimeRate)
                // Allow over-screen
                val moveLimitRect = floatingView.mMoveLimitRect
                // Final destination
                val targetPositionX = min(max(moveLimitRect.left, mTouchPositionX.toInt()), moveLimitRect.right).toFloat()
                val targetPositionY = min(max(moveLimitRect.top, mTouchPositionY.toInt()), moveLimitRect.bottom).toFloat()
                params.x = (mStartX + (targetPositionX - mStartX) * basePosition).toInt()
                params.y = (mStartY + (targetPositionY - mStartY) * basePosition).toInt()
                floatingView.updateViewLayout()
                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS)
            } else if (mState == FloatingView.STATE_INTERSECTING) {
                val basePosition = calcAnimationPosition(trackingTargetTimeRate)
                // Final destination
                val targetPositionX = mTargetPositionX - floatingView.width / 2
                val targetPositionY = mTargetPositionY - floatingView.height / 2
                // Move from your current location
                params.x = (mStartX + (targetPositionX - mStartX) * basePosition).toInt()
                params.y = (mStartY + (targetPositionY - mStartY) * basePosition).toInt()
                floatingView.updateViewLayout()
                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS)
            } // Animation when overlapping

        }

        /**
         * Send an animated message.
         *
         * @param animation   ANIMATION_IN_TOUCH
         * @param delayMillis Message sending time
         */
        fun sendAnimationMessageDelayed(animation: Int, delayMillis: Long) {
            sendMessageAtTime(
                newMessage(animation, TYPE_FIRST),
                SystemClock.uptimeMillis() + delayMillis
            )
        }

        /**
         * Send an animated message.
         *
         * @param animation ANIMATION_IN_TOUCH
         */
        fun sendAnimationMessage(animation: Int) {
            sendMessage(newMessage(animation, TYPE_FIRST))
        }

        /**
         * Update the position of the touch coordinates.
         *
         * @param positionX Touch X coordinates
         * @param positionY Touch Y coordinates
         */
        fun updateTouchPosition(positionX: Float, positionY: Float) {
            mTouchPositionX = positionX
            mTouchPositionY = positionY
        }

        /**
         * Update the position of the tracking target.
         *
         * @param centerX X coordinate of the tracking target
         * @param centerY Y coordinate of the tracking target
         */
        fun updateTargetPosition(centerX: Float, centerY: Float) {
            mTargetPositionX = centerX
            mTargetPositionY = centerY
        }

        companion object {

            /**
             * Milliseconds to refresh the animation
             */
            private const val ANIMATION_REFRESH_TIME_MILLIS = 10L

            /**
             * Floating View adsorption attachment / detachment time
             */
            private const val CAPTURE_DURATION_MILLIS = 300L

            /**
             * Constant representing the unanimated state
             */
            private const val ANIMATION_NONE = 0

            /**
             * Animation constants that occur on touch
             */
            const val ANIMATION_IN_TOUCH = 1

            /**
             * Constant representing the start of animation
             */
            private const val TYPE_FIRST = 1
            /**
             * Constant representing animation update
             */
            private const val TYPE_UPDATE = 2

            /**
             * Calculate the position obtained from the animation time.
             *
             * @param timeRate time rate
             * @return Base factor (0.0 to 1.0 + α)
             */
            private fun calcAnimationPosition(timeRate: Float): Float {
                // y=0.55sin(8.0564x-π/2)+0.55
                // y=4(0.417x-0.341)^2-4(0.417-0.341)^2+1
                return if (timeRate <= 0.4) {
                    (0.55 * sin(8.0564 * timeRate - Math.PI / 2) + 0.55).toFloat()
                } else {
                    (4 * (0.417 * timeRate - 0.341).pow(2.0) - 4 * (0.417 - 0.341).pow(2.0) + 1).toFloat()
                }
            }

            /**
             * Generate a message to send.
             *
             * @param animation ANIMATION_IN_TOUCH
             * @param type      TYPE_FIRST,TYPE_UPDATE
             * @return Message
             */
            private fun newMessage(animation: Int, type: Int): Message {
                val message = Message.obtain()
                message.what = animation
                message.arg1 = type
                return message
            }
        }
    }

    /**
     * A handler that controls long press processing. <br> </br>
     * Since all touch processing is implemented by dispatchTouchEvent, long press is also implemented independently.
     */
    internal class LongPressHandler
    /**
     * Constructor
     *
     * @param view FloatingView
     */
        (view: FloatingView) : Handler() {

        /**
         * TrashView
         */
        private val mFloatingView: WeakReference<FloatingView> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            val view = mFloatingView.get()
            if (view == null) {
                removeMessages(LONG_PRESSED)
                return
            }

            view.onLongClick()
        }

        companion object {

            /**
             * Constant representing the unanimated state
             */
            const val LONG_PRESSED = 0
        }
    }

    fun setRealTimePositionListener(realTimePositionListener: RealTimePositionListener) {
        this.realTimePositionListener = realTimePositionListener
    }

    internal interface RealTimePositionListener {
        fun currentPosition()
    }

    companion object {

        /**
         * Enlargement rate when pressed
         */
        private const val SCALE_PRESSED = 0.9f

        /**
         * Normal magnification
         */
        private const val SCALE_NORMAL = 1.0f

        /**
         * Screen edge movement animation time
         */
        private const val MOVE_TO_EDGE_DURATION = 450L

        /**
         * Coefficient of screen edge movement animation
         */
        private const val MOVE_TO_EDGE_OVERSHOOT_TENSION = 1.25f

        /**
         * Damping ratio constant for spring animation (X coordinate)
         */
        private const val ANIMATION_SPRING_X_DAMPING_RATIO = 0.7f

        /**
         * Stiffness constant for spring animation (X coordinate)
         */
        private const val ANIMATION_SPRING_X_STIFFNESS = 350f

        /**
         * Friction constant for fling animation (X coordinate)
         */
        private const val ANIMATION_FLING_X_FRICTION = 1.7f

        /**
         * Friction constant for fling animation (Y coordinate)
         */
        private const val ANIMATION_FLING_Y_FRICTION = 1.7f

        /**
         * Current velocity units
         */
        private const val CURRENT_VELOCITY_UNITS = 1000

        /**
         * Normal state
         */
        const val STATE_NORMAL = 0

        /**
         * Overlapping state
         */
        const val STATE_INTERSECTING = 1

        /**
         * Termination status
         */
        const val STATE_FINISHING = 2

        /**
         * Time for long press judgment (1.5 times normal considering movement operation)
         */
        private val LONG_PRESS_TIMEOUT = (1.5f * ViewConfiguration.getLongPressTimeout()).toInt()

        /**
         * Constant for scaling down X coordinate velocity
         */
        private const val MAX_X_VELOCITY_SCALE_DOWN_VALUE = 9f

        /**
         * Constant for scaling down Y coordinate velocity
         */
        private const val MAX_Y_VELOCITY_SCALE_DOWN_VALUE = 8f

        /**
         * Constant for calculating the threshold to move when throwing
         */
        private const val THROW_THRESHOLD_SCALE_DOWN_VALUE = 9f

        /**
         * A value that represents the default X coordinate
         */
        const val DEFAULT_X = Integer.MIN_VALUE

        /**
         * A value that represents the default Y coordinate
         */
        const val DEFAULT_Y = Integer.MIN_VALUE

        /**
         * Default width size
         */
        const val DEFAULT_WIDTH = ViewGroup.LayoutParams.WRAP_CONTENT

        /**
         * Default height size
         */
        const val DEFAULT_HEIGHT = ViewGroup.LayoutParams.WRAP_CONTENT

        /**
         * Overlay Type
         */
        private val OVERLAY_TYPE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
        }

        /**
         * Get the System ui dimension(pixel)
         *
         * @param resources [Resources]
         * @param resName   dimension resource name
         * @return pixel size
         */
        private fun getSystemUiDimensionPixelSize(resources: Resources, resName: String): Int {
            var pixelSize = 0
            val resId = resources.getIdentifier(resName, "dimen", "android")
            if (resId > 0) {
                pixelSize = resources.getDimensionPixelSize(resId)
            }
            return pixelSize
        }
    }
}
