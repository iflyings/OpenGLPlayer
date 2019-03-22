package com.android.iflyings.player.model

import android.graphics.Rect

data class WindowData(private var windowRect: Rect?) {
    var screenWidth = 0
        private set
    var screenHeight = 0
        private set

    val windowLeft
        get() = windowRect?.left ?: 0
    val windowTop
        get() = windowRect?.top ?: 0
    val windowWidth
        get() = windowRect?.width() ?: screenWidth
    val windowHeight
        get() = windowRect?.height() ?: screenHeight

    fun setScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }
    fun setWindowRect(rect: Rect?) {
        windowRect = rect
    }

    override fun toString(): String {
        return "WindowData: {Screen=[$screenWidth,$screenHeight]," +
                "Window=[$windowLeft,$windowTop,$windowWidth,$windowHeight]}"
    }
}