package com.yenaly.han1meviewer.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ActivitySettingsBinding
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.screen.settings.SettingsActivityContent
import com.yenaly.han1meviewer.ui.screen.settings.SettingsDestinationSpec
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel
import com.yenaly.yenaly_libs.base.YenalyActivity

class SettingsActivity : YenalyActivity<ActivitySettingsBinding>(), ToolbarHost {

    val viewModel by viewModels<SettingsViewModel>()
    var navigateBackAction: () -> Unit = { onBackPressedDispatcher.onBackPressed() }

    private val startDestination: SettingsDestinationSpec by lazy {
        SettingsRouter.resolveStartDestination(intent)
    }

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySettingsBinding =
        ActivitySettingsBinding.inflate(layoutInflater)

    override fun setUiStyle() {
        enableEdgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun initData(savedInstanceState: Bundle?) {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeActionContentDescription(R.string.back)
        }
        binding.toolbar.setNavigationOnClickListener {
            navigateBackAction()
        }
        (binding.fcvSettings as ComposeView).setContent {
            HanimeTheme {
                SettingsActivityContent(
                    activity = this,
                    startDestination = startDestination,
                    onExitSettings = {
                        onBackPressedDispatcher.onBackPressed()
                    },
                )
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fcvSettings) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        hideToolbar()
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    override fun setupToolbar(title: CharSequence, canNavigateBack: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(canNavigateBack)
        }
        toolbar.setNavigationOnClickListener {
            navigateBackAction()
        }
    }

    override fun hideToolbar() {
        binding.toolbar.visibility = View.GONE
    }

    override fun showToolbar() {
        binding.toolbar.visibility = View.VISIBLE
    }
}
