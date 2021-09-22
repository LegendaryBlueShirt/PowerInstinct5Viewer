package com.justnopoint.interfaces

abstract class Sequence {
    abstract fun getName(): String
    abstract fun getFramecount(): Int
    abstract fun getFrames(): List<Frame>

    open fun getLoopPoint(): Int {
        return 0
    }

    override fun toString(): String {
        return getName()
    }
}