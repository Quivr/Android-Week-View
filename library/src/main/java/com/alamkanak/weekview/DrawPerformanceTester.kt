package com.alamkanak.weekview

import android.util.Log

class DrawPerformanceTester(val measureDrawTime: Boolean = true) {
    var drawSamplesCount = 0L
    var drawTotalTime = 0L

    private var startTime: Long = 0L

    fun startMeasure() {
        if (!measureDrawTime)
            return
        startTime = System.currentTimeMillis()
    }

    fun endMeasure() {
        if (!measureDrawTime)
            return
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        ++drawSamplesCount
        drawTotalTime += totalTime
        val drawAverageTime = drawTotalTime.toFloat() / drawSamplesCount.toFloat()
        Log.d("AppLog", "currentTime:$totalTime average:$drawAverageTime")
    }
}
