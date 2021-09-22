package com.justnopoint.util

import com.justnopoint.interfaces.Sequence

object AnimHelper {

    fun getLoopingDurationTotal(sequence: Sequence?): Int {
        if(sequence == null) {
            return -1
        }
        var total = 0
        sequence.getFrames().forEachIndexed { index, frame ->
            if(index >= sequence.getLoopPoint()) {
                total += frame.getDuration()
            }
        }
        return total
    }

    fun getSequenceDurationTotal(sequence: Sequence?): Int {
        if(sequence == null) {
            return -1
        }
        var total = 0
        for (frame in sequence.getFrames()) {
            total += frame.getDuration()
        }
        return total
    }

    fun getTimeForFrame(sequence: Sequence?, frame: Int): Int {
        if(sequence == null) {
            return -1
        }
        var time = 0
        for (n in 0 until frame) {
            time += sequence.getFrames()[n].getDuration()
        }
        return time
    }

    fun getFrameForTime(sequence: Sequence?, time: Int, useLoopPoint: Boolean = false): Int {
        if(sequence == null) {
            return -1
        }
        val totalDuration = getSequenceDurationTotal(sequence)
        if(totalDuration <= 0) {
            return -1
        }
        var currentTime = time
        if(useLoopPoint) {
            while(currentTime > totalDuration) {
                currentTime -= getLoopingDurationTotal(sequence)
            }
        } else {
            currentTime = time % getSequenceDurationTotal(sequence)
        }
        var frameNum = -1
        for (frame in sequence.getFrames()) {
            frameNum++
            currentTime -= frame.getDuration()
            if (currentTime < 0)
                break
        }
        return frameNum
    }
}