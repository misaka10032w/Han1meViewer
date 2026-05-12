package com.yenaly.han1meviewer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.settings.SettingsDestinationSpec
import com.yenaly.yenaly_libs.utils.activity

class SettingsRouter private constructor(
    private val context: Context,
) {
    companion object {
        const val DESTINATION = "destination"
        const val DESTINATION_ROUTE = "destination_route"

        @JvmStatic
        fun with(fragment: Fragment) = SettingsRouter(fragment.requireContext())

        fun resolveStartDestination(intent: Intent): SettingsDestinationSpec {
            val legacyId = intent.getIntExtra(DESTINATION, 0)
            SettingsDestinationSpec.fromLegacyId(legacyId)?.let { return it }
            val routeKey = intent.getStringExtra(DESTINATION_ROUTE)
            return SettingsDestinationSpec.fromRouteKey(routeKey) ?: SettingsDestinationSpec.Home
        }
    }

    fun toSettingsActivity(
        @IdRes id: Int = 0,
        destination: SettingsDestinationSpec? = null,
    ) {
        val activity = context.activity ?: return
        val resolvedDestination = destination ?: SettingsDestinationSpec.fromLegacyId(id)
        val intent = Intent(activity, SettingsActivity::class.java).apply {
            if (id != 0) {
                putExtra(DESTINATION, id)
            }
            resolvedDestination?.let {
                putExtra(DESTINATION_ROUTE, it.routeKey)
            }
        }
        activity.startActivity(intent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    fun navigateWithinSettings(
        @IdRes to: Int,
        args: Bundle? = null,
        inclusive: Boolean = false,
    ) {
        toSettingsActivity(id = to)
    }
}
