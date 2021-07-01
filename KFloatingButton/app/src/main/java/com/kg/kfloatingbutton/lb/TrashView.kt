package com.kg.kfloatingbutton.lb

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * FloatingViewを消すためのViewです。
 */
internal class TrashView
/**
 * Constructor
 *
 * @param context Context
 */
(context: Context) : FrameLayout(context), ViewTreeObserver.OnPreDrawListener {

    /**
     * WindowManager
     */
    private val mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * LayoutParams
     */
    /**
     * WindowManager.LayoutParams
     *
     * @return WindowManager.LayoutParams
     */
    val windowLayoutParams: WindowManager.LayoutParams

    /**
     * DisplayMetrics
     */
    private val mMetrics: DisplayMetrics = DisplayMetrics()

    /**
     * Root View (View with background and delete icon)
     */
    private val mRootView: ViewGroup

    /**
     * Delete icon
     */
    private val mTrashIconRootView: FrameLayout

    /**
     * Fixed delete icon
     */
    private val mFixedTrashIconView: ImageView

    /**
     * Delete icon that works according to overlap
     */
    private val mActionTrashIconView: ImageView

    /**
     * ActionTrashIcon width
     */
    private var mActionTrashIconBaseWidth: Int = 0

    /**
     * ActionTrashIcon height
     */
    private var mActionTrashIconBaseHeight: Int = 0

    /**
     * Maximum magnification of ActionTrashIcon
     */
    private var mActionTrashIconMaxScale: Float = 0.toFloat()

    /**
     * Background View
     */
    private  val mBackgroundView :  FrameLayout

    /**
     * Animation when entering the frame of the delete icon (enlarge)
     */
    private  var mEnterScaleAnimator :  ObjectAnimator ?  =  null

    /**
     * Animation (reduction) when the delete icon goes out of the frame
     */
    private var mExitScaleAnimator: ObjectAnimator? = null

    /**
     * Handler to animate
     */
    private val mAnimationHandler: AnimationHandler

    /**
     * TrashViewListener
     */
    private var mTrashViewListener: TrashViewListener? = null

    /**
     * View enable / disable flag (not displayed if disabled)
     */
    private var mIsEnabled: Boolean = false

    /**
     * Gets the center X coordinate of the delete icon.
     *
     * @return Center X coordinate of delete icon
     */
    val trashIconCenterX: Float
        get() {
            val iconView = if (hasActionTrashIcon()) mActionTrashIconView else mFixedTrashIconView
            val iconViewPaddingLeft = iconView.paddingLeft.toFloat()
            val iconWidth = iconView.width.toFloat() - iconViewPaddingLeft - iconView.paddingRight.toFloat()
            val x = mTrashIconRootView.x + iconViewPaddingLeft
            return x + iconWidth / 2
        }

    /**
     * Gets the center Y coordinate of the delete icon.
     *
     * @return Center Y coordinate of delete icon
     */
    val trashIconCenterY: Float
        get() {
            val iconView = if (hasActionTrashIcon()) mActionTrashIconView else mFixedTrashIconView
            val iconViewHeight = iconView.height.toFloat()
            val iconViewPaddingBottom = iconView.paddingBottom.toFloat()
            val iconHeight = iconViewHeight - iconView.paddingTop.toFloat() - iconViewPaddingBottom
            val y = mRootView.height.toFloat() - mTrashIconRootView.y - iconViewHeight + iconViewPaddingBottom
            return y + iconHeight / 2
        }

    /**
     * Get the display status of TrashView.
     *
     * @return Display if true
     */
    /**
     * Enable / disable TrashView.
     *
     * @param enabled If true, it is enabled (displayed), if false, it is disabled (hidden)
     */
    // Do nothing if the settings are the same
    // Close to hide
    var isTrashEnabled: Boolean
        get() = mIsEnabled
        set(enabled) {
            if (mIsEnabled == enabled) {
                return
            }
            mIsEnabled = enabled
            if (!mIsEnabled) {
                dismiss()
            }
        }

    /**
     * Animation State
     */
    @IntDef(ANIMATION_NONE, ANIMATION_OPEN, ANIMATION_CLOSE, ANIMATION_FORCE_CLOSE)
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class AnimationState

    init {
        mWindowManager.defaultDisplay.getMetrics(mMetrics)
        mAnimationHandler = AnimationHandler(this)
        mIsEnabled = true

        windowLayoutParams = WindowManager.LayoutParams()
        windowLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        windowLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        windowLayoutParams.type = OVERLAY_TYPE
        windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        windowLayoutParams.format = PixelFormat.TRANSLUCENT
        // INFO: Set only the origin of Window to the lower left
        windowLayoutParams.gravity = Gravity.START or Gravity.BOTTOM

        // Various View settings
        // View that can be pasted directly into TrashView (The layout of the deleted view and background view is broken for some reason without going through this view)
        mRootView = FrameLayout(context)
        mRootView.setClipChildren(false)
        // Delete icon root View
        mTrashIconRootView = FrameLayout(context)
        mTrashIconRootView.clipChildren = false
        mFixedTrashIconView = ImageView(context)
        mActionTrashIconView = ImageView(context)
        // Background View
        mBackgroundView = FrameLayout(context)
        mBackgroundView.alpha = 0.0f
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00000000, 0x50000000))
        mBackgroundView.background = gradientDrawable

        // Paste background view
        val backgroundParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (BACKGROUND_HEIGHT * mMetrics.density).toInt())
        backgroundParams.gravity = Gravity.BOTTOM
        mRootView.addView(mBackgroundView, backgroundParams)
        // Paste action icon
        val actionTrashIconParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        actionTrashIconParams.gravity = Gravity.CENTER
        mTrashIconRootView.addView(mActionTrashIconView, actionTrashIconParams)
        // Paste fixed icon
        val fixedTrashIconParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        fixedTrashIconParams.gravity = Gravity.CENTER
        mTrashIconRootView.addView(mFixedTrashIconView, fixedTrashIconParams)
        // Paste the delete icon
        val trashIconParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        trashIconParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        mRootView.addView(mTrashIconRootView, trashIconParams)

        // Paste into TrashView
        addView(mRootView)

        // For initial drawing process
        viewTreeObserver.addOnPreDrawListener(this)
    }

    /**
     * Determine the display position.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewLayout()
    }

    /**
     * Adjust the layout when the screen is rotated.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateViewLayout()
    }

    /**
     * Set the coordinates for the first drawing. <br> </br>
     * Because there is an event that the delete icon is displayed for a moment when it is displayed for the first time.
     */
    override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)
        mTrashIconRootView.translationY = mTrashIconRootView.measuredHeight.toFloat()
        return true
    }

    /**
     * initialize ActionTrashIcon
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mTrashViewListener!!.onUpdateActionTrashIcon()
    }

    /**
     * Determine your position from the screen size.
     */
    private fun updateViewLayout() {
        mWindowManager.defaultDisplay.getMetrics(mMetrics)
        windowLayoutParams.x = (mMetrics.widthPixels - width) / 2
        windowLayoutParams.y = 0

        // Update view and layout
        mTrashViewListener!!.onUpdateActionTrashIcon()
        mAnimationHandler.onUpdateViewLayout ()

        mWindowManager.updateViewLayout(this, windowLayoutParams)
    }

    /**
     * Hide TrashView.
     */
    fun  dismiss () {
        // Stop animation
        mAnimationHandler.removeMessages(ANIMATION_OPEN)
        mAnimationHandler.removeMessages(ANIMATION_CLOSE)
        mAnimationHandler.sendAnimationMessage(ANIMATION_FORCE_CLOSE)
        // Stop magnifying animation
        setScaleTrashIconImmediately ( false )
    }


    /**
     * Gets the drawing area on the Window.
     * Represents a collision detection rectangle.
     *
     * @param outRect Make changes Rect
     */
    fun getWindowDrawingRect(outRect: Rect) {
        // Since Gravity is in the opposite direction, the collision detection of the rectangle is also upside down (top / bottom)
        // Set a lot of top (downward on the screen) judgment
        val iconView = if (hasActionTrashIcon()) mActionTrashIconView else mFixedTrashIconView
        val iconPaddingLeft = iconView.paddingLeft.toFloat()
        val iconPaddingTop = iconView.paddingTop.toFloat()
        val iconWidth = iconView.width.toFloat() - iconPaddingLeft - iconView.paddingRight.toFloat()
        val iconHeight = iconView.height.toFloat() - iconPaddingTop - iconView.paddingBottom.toFloat()
        val x = mTrashIconRootView.x + iconPaddingLeft
        val y = mRootView.height.toFloat() - mTrashIconRootView.y - iconPaddingTop - iconHeight
        val left = (x - TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density).toInt()
        val top = -mRootView.height
        val right = (x + iconWidth + TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density).toInt()
        val bottom = (y + iconHeight + TARGET_CAPTURE_VERTICAL_REGION * mMetrics.density).toInt()
        outRect.set(left, top, right, bottom)
    }

    /**
     * Update the settings of the delete icon to take action.
     *
     * @param width The width of the target View
     * @param height The height of the target View
     * @param shape The shape of the target View
     */
    fun updateActionTrashIcon(width: Float, height: Float, shape: Float) {
        // Do nothing if the delete icon to take action is not set
        if (!hasActionTrashIcon()) {
            return
        }
        // Setting the magnification
        mAnimationHandler.mTargetWidth = width
        mAnimationHandler.mTargetHeight = height
        val newWidthScale = width / mActionTrashIconBaseWidth * shape
        val newHeightScale = height / mActionTrashIconBaseHeight * shape
        mActionTrashIconMaxScale = max(newWidthScale, newHeightScale)
        // ENTER Animation creation
        mEnterScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, mActionTrashIconMaxScale), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, mActionTrashIconMaxScale))
        mEnterScaleAnimator !! .interpolator =  OvershootInterpolator ()
        mEnterScaleAnimator!!.duration = TRASH_ICON_SCALE_DURATION_MILLIS
        // Create Exit animation
        mExitScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, 1.0f), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, 1.0f))
        mExitScaleAnimator!!.interpolator = OvershootInterpolator()
        mExitScaleAnimator!!.duration = TRASH_ICON_SCALE_DURATION_MILLIS
    }


    /**
     * Check if there is a delete icon to take action.
     *
     * @return true if there is a delete icon to take action
     */
    private fun hasActionTrashIcon(): Boolean {
        return mActionTrashIconBaseWidth != 0 && mActionTrashIconBaseHeight != 0
    }

    /**
     * Set the image of the fixed delete icon. <br> </br>
     * This image does not change in size when floating displays overlap.
     *
     * @param resId drawable ID
     */
    fun setFixedTrashIconImage(resId: Int) {
        mFixedTrashIconView.setImageResource(resId)
    }

    /**
     * Set the image of the delete icon to take action. <br> </br>
     * This image will change size when the floating display overlaps.
     *
     * @param resId drawable ID
     */
    fun setActionTrashIconImage(resId: Int) {
        mActionTrashIconView.setImageResource(resId)
        val drawable = mActionTrashIconView.drawable
        if (drawable != null) {
            mActionTrashIconBaseWidth = drawable.intrinsicWidth
            mActionTrashIconBaseHeight = drawable.intrinsicHeight
        }
    }

    /**
     * Set the fixed delete icon. <br> </br>
     * This image does not change in size when floating displays overlap.
     *
     * @param drawable Drawable
     */
    fun setFixedTrashIconImage(drawable: Drawable) {
        mFixedTrashIconView.setImageDrawable (drawable)
    }

    /**
     * Set the delete icon for action. <br> </br>
     * This image will change size when the floating display overlaps.
     *
     * @param drawable Drawable
     */
    fun setActionTrashIconImage(drawable: Drawable?) {
        mActionTrashIconView.setImageDrawable (drawable)
        if (drawable != null) {
            mActionTrashIconBaseWidth = drawable.intrinsicWidth
            mActionTrashIconBaseHeight = drawable.intrinsicHeight
        }
    }

    /**
     * Immediately resize the delete icon.
     *
     * @param isEnter true if entered area, false otherwise
     */
    private fun setScaleTrashIconImmediately(isEnter: Boolean) {
        cancelScaleTrashAnimation ()

        mActionTrashIconView.scaleX = if (isEnter) mActionTrashIconMaxScale else 1.0f
        mActionTrashIconView.scaleY = if (isEnter) mActionTrashIconMaxScale else 1.0f
    }

    /**
     * Change the size of the delete icon.
     *
     * @param isEnter true if entered area, false otherwise
     */
    fun setScaleTrashIcon(isEnter: Boolean) {
        // Do nothing if the action icon is not set
        if (!hasActionTrashIcon()) {
            return
        }

        // Cancel animation
        cancelScaleTrashAnimation ()

        // When entering the area
        if (isEnter) {
            mEnterScaleAnimator !! .start ()
        } else {
            mExitScaleAnimator!!.start()
        }
    }

    /**
     * Cancel the enlargement / reduction animation of the delete icon
     */
    private  fun  cancelScaleTrashAnimation () {
        // Animation in the frame
        if (mEnterScaleAnimator != null && mEnterScaleAnimator!!.isStarted) {
            mEnterScaleAnimator !! .cancel ()
        }

        // Out-of-frame animation
        if (mExitScaleAnimator != null && mExitScaleAnimator!!.isStarted) {
            mExitScaleAnimator!!.cancel()
        }
    }

    /**
     * Set TrashViewListener.
     *
     * @param listener TrashViewListener
     */
    fun setTrashViewListener(listener: TrashViewListener) {
        mTrashViewListener = listener
    }


    /**
     * Performs processing related to FloatingView.
     *
     * @param event MotionEvent
     * @param x FloatingView X coordinate
     * @param y FloatingView Y coordinate
     */
    fun onTouchFloatingView(event: MotionEvent, x: Float, y: Float) {
        val action = event.action
        // Press
        if (action == MotionEvent.ACTION_DOWN) {
            mAnimationHandler.updateTargetPosition(x, y)
            // Wait for long press processing
            mAnimationHandler.removeMessages(ANIMATION_CLOSE)
            mAnimationHandler.sendAnimationMessageDelayed(ANIMATION_OPEN, LONG_PRESS_TIMEOUT.toLong())
        } else if (action == MotionEvent.ACTION_MOVE) {
            mAnimationHandler.updateTargetPosition(x, y)
            // Run only if the open animation has not started yet
            if (!mAnimationHandler.isAnimationStarted(ANIMATION_OPEN)) {
                // Delete long press message
                mAnimationHandler.removeMessages(ANIMATION_OPEN)
                // open
                mAnimationHandler.sendAnimationMessage ( ANIMATION_OPEN )
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Delete long press message
            mAnimationHandler.removeMessages(ANIMATION_OPEN)
            mAnimationHandler.sendAnimationMessage(ANIMATION_CLOSE)
        } // Push up, cancel
        // Move
    }

    /**
     * A handler that controls animation.
     */
    internal class AnimationHandler
    /**
     * Constructor
     */
        (trashView :  TrashView ) :  Handler () {

        /**
         * Time when the animation started
         */
        private var mStartTime: Long = 0

        /**
         * Alpha value at the beginning of the animation
         */
        private var mStartAlpha: Float = 0.toFloat()

        /**
         * TransitionY at the beginning of the animation
         */
        private var mStartTransitionY: Float = 0.toFloat()

        /**
         * Running animation code
         */
        private var mStartedCode: Int = 0

        /**
         * X coordinate of the tracking target
         */
        private var mTargetPositionX: Float = 0.toFloat()

        /**
         * Y coordinate of the tracking target
         */
        private var mTargetPositionY: Float = 0.toFloat()

        /**
         * Width of tracking target
         */
        var mTargetWidth: Float = 0.toFloat()

        /**
         * Height of tracking target
         */
        var mTargetHeight: Float = 0.toFloat()

        /**
         * Move limit position of delete icon
         */
        private val mTrashIconLimitPosition: Rect

        /**
         * Y-axis tracking range
         */
        private var mMoveStickyYRange: Float = 0.toFloat()

        /**
         * OvershootInterpolator
         */
        private val mOvershootInterpolator: OvershootInterpolator



        /**
         * TrashView
         */
        private val mTrashView: WeakReference<TrashView> = WeakReference(trashView)

        init {
            mStartedCode = ANIMATION_NONE
            mTrashIconLimitPosition = Rect()
            mOvershootInterpolator = OvershootInterpolator(OVERSHOOT_TENSION)
        }

        /**
         * Performs animation processing.
         */
        override fun handleMessage(msg: Message) {
            val trashView = mTrashView.get()
            if (trashView == null) {
                removeMessages(ANIMATION_OPEN)
                removeMessages(ANIMATION_CLOSE)
                removeMessages(ANIMATION_FORCE_CLOSE)
                return
            }

            // Do not animate if not valid
            if (!trashView.isTrashEnabled) {
                return
            }

            val animationCode = msg.what
            val animationType = msg.arg1
            val backgroundView = trashView.mBackgroundView
            val trashIconRootView = trashView.mTrashIconRootView
            val listener = trashView.mTrashViewListener
            val screenWidth = trashView.mMetrics.widthPixels.toFloat()
            val trashViewX = trashView.windowLayoutParams.x.toFloat()

            // Initialization when animation is started
            if (animationType == TYPE_FIRST) {
                mStartTime = SystemClock.uptimeMillis()
                mStartAlpha = backgroundView.alpha
                mStartTransitionY = trashIconRootView.translationY
                mStartedCode = animationCode
                listener?.onTrashAnimationStarted(mStartedCode)
            }
            // elapsed time
            val elapsedTime = (SystemClock.uptimeMillis() - mStartTime).toFloat()

            // Display animation
            if (animationCode == ANIMATION_OPEN) {
                val currentAlpha = backgroundView.alpha
                // If the maximum alpha value has not been reached
                if (currentAlpha < MAX_ALPHA) {
                    val alphaTimeRate = min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f)
                    val alpha = min(mStartAlpha + alphaTimeRate, MAX_ALPHA)
                    backgroundView.alpha = alpha
                }

                // Start animation if Delay Time is exceeded
                if (elapsedTime >= TRASH_OPEN_START_DELAY_MILLIS) {
                    val screenHeight = trashView.mMetrics.heightPixels.toFloat()
                    // Calculate 0% and 100% respectively if all the icons extend to the left and right
                    val positionX = trashViewX + (mTargetPositionX + mTargetWidth) / (screenWidth + mTargetWidth) * mTrashIconLimitPosition.width() + mTrashIconLimitPosition.left.toFloat()
                    // Y coordinate animation and follow of delete icon (upward is minus)
                    // targetPositionYRate is 0% when the Y coordinate of the target is completely off the screen, and 100% after half of the screen.
                    // stickyPositionY moves from the lower end of the movement limit to the upper end at the origin. mMoveStickyRange is the tracking range
                    // Move over time by calculating positionY
                    val targetPositionYRate = min(2 * (mTargetPositionY + mTargetHeight) / (screenHeight + mTargetHeight), 1.0f)
                    val stickyPositionY = mMoveStickyYRange * targetPositionYRate + mTrashIconLimitPosition.height() - mMoveStickyYRange
                    val translationYTimeRate = min((elapsedTime - TRASH_OPEN_START_DELAY_MILLIS) / TRASH_OPEN_DURATION_MILLIS, 1.0f)
                    val positionY = mTrashIconLimitPosition.bottom - stickyPositionY * mOvershootInterpolator.getInterpolation(translationYTimeRate)
                    trashIconRootView.translationX = positionX
                    trashIconRootView.translationY = positionY
                    // clear drag view garbage
                }

                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS)
            } else if (animationCode == ANIMATION_CLOSE) {
                // Alpha value calculation
                val alphaElapseTimeRate = min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f)
                val alpha = max(mStartAlpha - alphaElapseTimeRate, MIN_ALPHA)
                backgroundView.alpha = alpha

                // Y coordinate animation of delete icon
                val translationYTimeRate = min(elapsedTime / TRASH_CLOSE_DURATION_MILLIS, 1.0f)
                // If the animation has not reached the end
                if (alphaElapseTimeRate < 1.0f || translationYTimeRate < 1.0f) {
                    val position = mStartTransitionY + mTrashIconLimitPosition.height() * translationYTimeRate
                    trashIconRootView.translationY = position
                    sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS)
                } else {
                    // Force position adjustment
                    trashIconRootView.translationY = mTrashIconLimitPosition.bottom.toFloat()
                    mStartedCode = ANIMATION_NONE
                    listener?.onTrashAnimationEnd(ANIMATION_CLOSE)
                }
            } else if (animationCode == ANIMATION_FORCE_CLOSE) {
                backgroundView.alpha = 0.0f
                trashIconRootView.translationY = mTrashIconLimitPosition.bottom.toFloat()
                mStartedCode = ANIMATION_NONE
                listener?.onTrashAnimationEnd(ANIMATION_FORCE_CLOSE)
            } // Instant non-representation
            // Hidden animation
        }

        /**
         * Send an animated message.
         *
         * @param animation   ANIMATION_OPEN,ANIMATION_CLOSE,ANIMATION_FORCE_CLOSE
         * @param delayMillis Message sending time
         */
        fun sendAnimationMessageDelayed(animation: Int, delayMillis: Long) {
            sendMessageAtTime(newMessage(animation, TYPE_FIRST), SystemClock.uptimeMillis() + delayMillis)
        }

        /**
         * Send an animated message.
         *
         * @param animation ANIMATION_OPEN,ANIMATION_CLOSE,ANIMATION_FORCE_CLOSE
         */
        fun sendAnimationMessage(animation: Int) {
            sendMessage(newMessage(animation, TYPE_FIRST))
        }

        /**
         * Check if the animation has started.
         *
         * @param animationCode Animation code
         * @return true if the animation has started, false otherwise
         */
        fun isAnimationStarted(animationCode: Int): Boolean {
            return mStartedCode == animationCode
        }

        /**
         * Updates the position information of the tracking target.
         *
         * @param x X coordinate to follow
         * @param y Y coordinate to be followed
         */
        fun updateTargetPosition(x: Float, y: Float) {
            mTargetPositionX = x
            mTargetPositionY = y
        }


        /**
         * Called when the view status changes.
         */
        fun onUpdateViewLayout() {
            val trashView = mTrashView.get() ?: return
// Move limit setting of delete icon (TrashIconRootView) (calculated based on Gravity reference position)
            // At the lower left origin (bottom of screen (including padding): 0, upward: minus, downward: plus), the upper limit of the Y axis is the position where the delete icon comes to the center of the background, and the lower limit is the position where all TrashIconRootView is hidden.
            val density = trashView.mMetrics.density
            val backgroundHeight = trashView.mBackgroundView.measuredHeight.toFloat()
            val offsetX = TRASH_MOVE_LIMIT_OFFSET_X * density
            val trashIconHeight = trashView.mTrashIconRootView.measuredHeight
            val left = (-offsetX).toInt()
            val top = ((trashIconHeight - backgroundHeight) / 2 - TRASH_MOVE_LIMIT_TOP_OFFSET * density).toInt()
            val right = offsetX.toInt()
            mTrashIconLimitPosition.set(left, top, right, trashIconHeight)

            // Set the Y-axis tracking range based on the size of the background
            mMoveStickyYRange = backgroundHeight * 0.20f
        }

        companion object {

            /**
             * Milliseconds to refresh the animation
             */
            private const val ANIMATION_REFRESH_TIME_MILLIS = 10L

            /**
             * Background animation time
             */
            private const val BACKGROUND_DURATION_MILLIS = 200L

            /**
             * Start delay time of pop animation of delete icon
             */
            private const val TRASH_OPEN_START_DELAY_MILLIS = 200L

            /**
             * Delete icon open animation time
             */
            private const val TRASH_OPEN_DURATION_MILLIS = 400L

            /**
             * Delete icon close animation time
             */
            private const val TRASH_CLOSE_DURATION_MILLIS = 200L

            /**
             * Overshoot animation factor
             */
            private const val OVERSHOOT_TENSION = 1.0f

            /**
             * Delete icon movement limit X-axis offset (dp)
             */
            private const val TRASH_MOVE_LIMIT_OFFSET_X = 22

            /**
             * Delete icon movement limit Y-axis offset (dp)
             */
            private const val TRASH_MOVE_LIMIT_TOP_OFFSET = -4

            /**
             * Constant representing the start of animation
             */
            private const val TYPE_FIRST = 1
            /**
             * Constant representing animation update
             */
            private const val TYPE_UPDATE = 2

            /**
             * Maximum alpha
             */
            private const val MAX_ALPHA = 1.0f

            /**
             * Minimum alpha
             */
            private const val MIN_ALPHA = 0.0f

            /**
             * Clear the animation garbage of the target view.
             */
            private fun clearClippedChildren(viewGroup: ViewGroup) {
                viewGroup.clipChildren =  true
                viewGroup.invalidate()
                viewGroup.clipChildren =  false
            }

            /**
             * Generate a message to send.
             *
             * @param animation ANIMATION_OPEN,ANIMATION_CLOSE,ANIMATION_FORCE_CLOSE
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

    companion object {

        /**
         * Background height (dp)
         */
        private const val BACKGROUND_HEIGHT = 164

        /**
         * Horizontal area (dp) to capture the target
         */
        private const val TARGET_CAPTURE_HORIZONTAL_REGION = 30.0f

        /**
         * Vertical area (dp) to capture the target
         */
        private const val TARGET_CAPTURE_VERTICAL_REGION = 4.0f

        /**
         * Animation time for enlargement / reduction of delete icon
         */
        private const val TRASH_ICON_SCALE_DURATION_MILLIS = 200L

        /**
         * Constant representing the unanimated state
         */
        const val ANIMATION_NONE = 0
        /**
         * Constants that represent animations that display background / delete icons, etc. <br> </br>
         * Includes tracking of FloatingView.
         */
        const val ANIMATION_OPEN = 1
        /**
         * A constant that represents an animation that erases the background / delete icon, etc.
         */
        const val ANIMATION_CLOSE = 2
        /**
         * A constant that indicates that the background / delete icon, etc. should be deleted immediately.
         */
        const val ANIMATION_FORCE_CLOSE = 3

        /**
         * Time for long press judgment
         */
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()

        /**
         * Overlay Type
         */
        private val OVERLAY_TYPE: Int = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

    }
}
