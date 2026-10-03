package com.yenaly.han1meviewer.ui.adaptive

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistSelectionTest {
    @Test
    fun switchingListResetsPagingWithoutLosingSavedScrollPositions() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = ViewModelStore()
            val viewModel = ViewModelProvider(
                store,
                ViewModelProvider.NewInstanceFactory(),
            )[MyPlayListViewModelV2::class.java]
            try {
                viewModel.setListInfo("first", "First")
                viewModel.currentPage = 3
                viewModel.updatePlaylistSheetScrollState("first", 12, 34)

                viewModel.setListInfo("first", "Renamed")
                assertEquals(3, viewModel.currentPage)
                assertEquals("Renamed", viewModel.currentListInfo.value?.second)

                viewModel.setListInfo("second", "Second")
                assertEquals(1, viewModel.currentPage)
                assertEquals("second", viewModel.currentListInfo.value?.first)
                assertEquals(12, viewModel.getPlaylistSheetScrollState("first").firstVisibleItemIndex)
                assertEquals(34, viewModel.getPlaylistSheetScrollState("first").firstVisibleItemScrollOffset)
            } finally {
                store.clear()
            }
        }
    }
}
