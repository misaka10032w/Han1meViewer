package com.yenaly.han1meviewer.ui.adaptive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletWindowTest {
    @Test
    fun isTabletWindow_phonePortrait() {
        assertFalse(isTabletWindow(400, 360, false))
    }

    @Test
    fun isTabletWindow_phoneLandscapeWithoutToggle() {
        assertFalse(isTabletWindow(800, 360, false))
    }

    @Test
    fun isTabletWindow_phoneLandscapeWithToggle() {
        assertTrue(isTabletWindow(800, 360, true))
    }

    @Test
    fun isTabletWindow_realTablet() {
        assertTrue(isTabletWindow(800, 800, false))
    }

    @Test
    fun isTabletWindow_narrowWidth() {
        assertFalse(isTabletWindow(500, 800, false))
    }

    @Test
    fun shouldUseListDetail_threshold() {
        assertFalse(shouldUseListDetail(839))
        assertTrue(shouldUseListDetail(840))
    }

    @Test
    fun shouldUseYoutubeSplit_thresholds() {
        assertFalse(shouldUseYoutubeSplit(679, 800, false, false))
        assertFalse(shouldUseYoutubeSplit(680, 479, false, false))
        assertTrue(shouldUseYoutubeSplit(680, 480, false, false))
        assertFalse(shouldUseYoutubeSplit(1280, 800, true, false))
        assertFalse(shouldUseYoutubeSplit(1280, 800, false, true))
    }
}
