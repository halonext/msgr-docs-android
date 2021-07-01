package com.kg.kfloatingbutton.lb


/**
 * FloatingView listener.
 */
interface FloatingViewListener {

    /**
     * Called when exiting FloatingView.
     */
    fun onFinishFloatingView()

    /**
     * Callback when touch action finished.
     *
     * @param isFinishing Whether FloatingView is being deleted or not.
     * @param x           x coordinate
     * @param yy coordinate
     */
    fun onTouchFinished(isFinishing: Boolean, x: Int, y: Int)

}
