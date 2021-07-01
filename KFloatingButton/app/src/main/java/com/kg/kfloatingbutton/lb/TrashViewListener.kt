package com.kg.kfloatingbutton.lb


/**
 * A listener that handles TrashView events.
 * INFO: Due to the specification that the delete icon follows, the end of the OPEN animation will not be notified.
 */
internal interface TrashViewListener {

    /**
     * Require ActionTrashIcon updates.
     */
    fun onUpdateActionTrashIcon()

    /**
     * You will be notified when you start the animation.
     *
     * @param animationCode Animation code
     */
    fun onTrashAnimationStarted(animationCode: Int)

    /**
     * You will be notified when the animation is finished.
     *
     * @param animationCode Animation code
     */
    fun onTrashAnimationEnd(animationCode: Int)

}
