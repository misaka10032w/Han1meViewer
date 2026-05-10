package com.yenaly.han1meviewer.ui.fragment.dailycheckin

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.home.DailyCheckInScreen
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.widget.CheckInWidgetProvider
import com.yenaly.han1meviewer.util.openVideo

class DailyCheckInFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = 500L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = 500L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val hostActivity = requireActivity()
        return ComposeView(hostActivity).apply {
            setContent {
                HanimeTheme {
                    DailyCheckInScreen(
                        activity = hostActivity,
                        onBack = { findNavController().navigateUp() },
                        onAddWidget = { requestPinCheckInWidget() },
                        onNavigateToVideo = { code -> findNavController().openVideo(code) }
                    )
                }
            }
        }
    }

    private fun requestPinCheckInWidget() {
        val activity = requireActivity()
        val mgr = AppWidgetManager.getInstance(activity)
        Toast.makeText(activity, "部分rom不支持引导式添加，请手动添加小部件", Toast.LENGTH_SHORT).show()
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(
                ComponentName(activity, CheckInWidgetProvider::class.java),
                null,
                null
            )
        } else {
            Toast.makeText(activity, R.string.widget_not_supported, Toast.LENGTH_SHORT).show()
        }
    }
}
