package com.android.iflyings.player

import android.os.SystemClock
import android.view.animation.BounceInterpolator
import android.view.animation.Interpolator

class MediaScroller {
    private var mInterpolator: Interpolator = BounceInterpolator()

    private var mStartX: Float = 0f
    private var mFinalX: Float = 0f

    var currX: Float = 0f
        private set
    private var mStartTime: Long = 0
    private var mDuration: Int = 0
    private var mDurationReciprocal: Float = 0.toFloat()
    private var mDeltaX: Float = 0.toFloat()
    var isFinished: Boolean = false
        private set

    fun setInterpolator(interpolator: Interpolator) {
        mInterpolator = interpolator
    }

    fun forceFinished(finished: Boolean) {
        isFinished = finished
    }

    fun computeScrollOffset(): Boolean {
        if (isFinished) {
            return false
        }

        if (mStartX >= mFinalX) {
            currX = mFinalX
            isFinished = true
            return true
        }

        val timePassed = (SystemClock.uptimeMillis() - mStartTime).toInt()

        if (timePassed < mDuration) {
            val x = mInterpolator.getInterpolation(timePassed * mDurationReciprocal)
            currX = mStartX + x * mDeltaX
        } else {
            currX = mFinalX
            isFinished = true
        }
        return true
    }

    fun startScroll(currX: Float, duration: Int) {
        isFinished = false
        mDuration = Math.round(1f * duration * Math.abs(1f - currX))
        mStartTime = SystemClock.uptimeMillis() - duration + mDuration
        mStartX = 0f
        mFinalX = 1f
        mDeltaX = 1f
        mDurationReciprocal = 1.0f / duration.toFloat()
    }

    fun abortAnimation() {
        currX = mFinalX
        isFinished = true
    }

    internal class ViscousFluidInterpolator : Interpolator {

        override fun getInterpolation(input: Float): Float {
            val interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input)
            return if (interpolated > 0) {
                interpolated + VISCOUS_FLUID_OFFSET
            } else interpolated
        }

        companion object {
            /** Controls the viscous fluid effect (how much of it).  */
            private val VISCOUS_FLUID_SCALE = 8.0f

            private val VISCOUS_FLUID_NORMALIZE: Float
            private val VISCOUS_FLUID_OFFSET: Float

            init {

                // must be set to 1.0 (used in viscousFluid())
                VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f)
                // account for very small floating-point error
                VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f)
            }

            private fun viscousFluid(x: Float): Float {
                var x = x
                x *= VISCOUS_FLUID_SCALE
                if (x < 1.0f) {
                    x -= 1.0f - Math.exp((-x).toDouble()).toFloat()
                } else {
                    val start = 0.36787944117f   // 1/e == exp(-1)
                    x = 1.0f - Math.exp((1.0f - x).toDouble()).toFloat()
                    x = start + x * (1.0f - start)
                }
                return x
            }
        }
    }
}

