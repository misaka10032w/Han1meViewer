package com.yenaly.han1meviewer.ui.activity

import android.annotation.SuppressLint
import android.content.ClipData.Item
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.transition.TransitionManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.databinding.ActivityMainBinding
import com.yenaly.han1meviewer.hanimeSpannedTitle
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.fragment.home.HomePageFragment
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.logScreenViewEvent
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.han1meviewer.videoUrlRegex
import com.yenaly.yenaly_libs.base.YenalyActivity
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.showSnackBar
import com.yenaly.yenaly_libs.utils.startActivity
import com.yenaly.yenaly_libs.utils.textFromClipboard
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.descriptors.PrimitiveKind

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:35
 */
class MainActivity : YenalyActivity<ActivityMainBinding>(), DrawerListener, ToolbarHost {

    val viewModel by viewModels<MainViewModel>()

    private lateinit var navHostFragment: NavHostFragment
    lateinit var navController: NavController
    private var detailNavController: NavController? = null

    val currentFragment get() = navHostFragment.childFragmentManager.primaryNavigationFragment
    private val isTabletMode by lazy {
        resources.getBoolean(R.bool.isTablet)
    }

    // 登錄完了後讓activity刷新主頁
    private val loginDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.getHomePage()
                initHeaderView()
                initMenu()
            }
        }


    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    override fun setUiStyle() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
    }

    override val onFragmentResumedListener: (Fragment) -> Unit = { fragment ->
        logScreenViewEvent(fragment)
    }

    /**
     * 初始化数据
     */
    override fun initData(savedInstanceState: Bundle?) {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcv_main) as NavHostFragment
        navController = navHostFragment.navController
        if (!isTabletMode) {
            if (binding.dlMain is androidx.drawerlayout.widget.DrawerLayout) {
                (binding.dlMain as androidx.drawerlayout.widget.DrawerLayout).addDrawerListener(this)
            }
        }

        binding.nvMain.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true
        }

        //binding.nvMain.setupWithNavController(navController)
        // binding.dlMain.addDrawerListener(this)
        if (binding.dlMain is androidx.drawerlayout.widget.DrawerLayout) {
            (binding.dlMain as androidx.drawerlayout.widget.DrawerLayout).addDrawerListener(this)
        }
        setSupportActionBar(findViewById(R.id.toolbar))
        initHeaderView()
        //initNavActivity()
        initMenu()
        ViewCompat.setOnApplyWindowInsetsListener(binding.nvMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        // 设置导航控制器
        if (isTabletMode) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.fcv_main) as? NavHostFragment
            detailNavController = navHostFragment?.navController
        }
    }


    // 处理菜单项点击
    private fun handleNavigationItemSelected(menuItem: MenuItem) {
        // 登录检查（保留原有逻辑）
        if (loginNeededFragmentList.contains(menuItem.itemId) && !isAlreadyLogin) {
            showShortToast(R.string.login_first)
            return
        }

        when (menuItem.itemId) {
            // 主界面相关 - 这些在 nav_main.xml 中
            R.id.nv_home_page -> openInRightPane(R.id.nv_home_page)
            R.id.nv_watch_history -> openInRightPane(R.id.nv_watch_history)
            R.id.nv_fav_video -> openInRightPane(R.id.nv_fav_video)
            R.id.nv_playlist -> openInRightPane(R.id.nv_playlist)
            R.id.nv_watch_later -> openInRightPane(R.id.nv_watch_later)

            // 设置相关 - 这些在 nav_settings.xml 中
            R.id.nv_settings -> {
                // 导航到设置图的起始目的地
                navController.navigate(R.id.action_global_nav_settings)
            }

//            R.id.nv_h_keyframe_settings -> {
//                // 导航到设置图中的具体目的地
//              // navController.navigate(R.id.action_global_to_hKeyframeSettingsFragment)
//                val intent = Intent(this, SettingsActivity::class.java)
//                intent.putExtra("target", "hKeyframe")
//                startActivity(intent)
//            }

            // 特殊处理（下载）
            R.id.nv_download -> {
                startActivity<DownloadActivity>()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.fade_out)
                }
            }
        }

        // 在平板模式下不关闭左侧菜单
        if (!isTabletMode) {
            binding.dlMain?.closeDrawer(GravityCompat.START)
        }
    }

    // 在右侧内容区域打开目标
//    private fun openInRightPane(destinationId: Int) {
//        val options = if (isTabletMode) {
//            // 平板模式：替换当前内容
//            navOptions {
//                anim {
//                    enter = R.anim.fade_in
//                    exit = R.anim.fade_out
//                }
//                launchSingleTop = true
//            }
//        } else {
//            // 手机模式：正常导航
//            null
//        }
//
//        try {
//            navController.navigate(destinationId, null, options)
//        } catch (e: IllegalArgumentException) {
//            // 处理目标不在当前导航图中的情况
//            if (destinationId in settingsDestinations) {
//                // 设置相关目标在另一个导航图中
//                navController.navigate(R.id.action_global_nav_settings)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    detailNavController?.navigate(destinationId)
//                }, 100)
//            }
//        }
//    }
    private fun openInRightPane(destinationId: Int) {
        val options = navOptions {
            anim {
                enter = R.anim.fade_in
                exit = R.anim.fade_out
                popEnter = R.anim.fade_in
                popExit = R.anim.fade_out
            }
            launchSingleTop = true
        }

        try {
            navController.navigate(destinationId, null, options)
        } catch (e: IllegalArgumentException) {
            Log.e("Navigation", "Navigation destination not found: $destinationId", e)
        }
    }

    // 设置相关的目标ID
//    private val settingsDestinations = setOf(
//        R.id.homeSettingsFragment,
//        R.id.playerSettingsFragment,
//        R.id.hKeyframesFragment,
//        R.id.sharedHKeyframesFragment,
//        R.id.hKeyframeSettingsFragment,
//        R.id.networkSettingsFragment,
//        R.id.downloadSettingsFragment
//    )

    override fun onStart() {
        super.onStart()
        binding.root.post {
            textFromClipboard?.let {
                videoUrlRegex.find(it)?.groupValues?.get(1)?.let { videoCode ->
                    showFindRelatedLinkSnackBar(videoCode)
                }
            }
        }
    }

    override fun bindDataObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                AppViewModel.versionFlow.collect { state ->
                    if (state is WebsiteState.Success && Preferences.isUpdateDialogVisible) {
                        state.info?.let { release ->
                            Preferences.lastUpdatePopupTime = Clock.System.now().epochSeconds
                            showUpdateDialog(release)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.homePageFlow.collect { state ->
                    if (state is WebsiteState.Error) {
                        if (state.throwable is CloudFlareBlockedException) {
                            // TODO: 被屏蔽时的处理
                            Log.e("error", "被屏蔽时的处理")
                        }
                    }
                }
            }
        }

        binding.nvMain.getHeaderView(0)?.let { header ->
            val headerAvatar = header.findViewById<ImageView>(R.id.header_avatar)
            val headerUsername = header.findViewById<TextView>(R.id.header_username)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    viewModel.homePageFlow.collect { state ->
                        if (state is WebsiteState.Success) {
                            if (isAlreadyLogin) {
                                if (state.info.username == null) {
                                    headerAvatar.load(R.mipmap.ic_launcher) {
                                        crossfade(true)
                                        transformations(CircleCropTransformation())
                                    }
                                    headerUsername.setText(R.string.refresh_page_or_login_expired)
                                } else {
                                    headerAvatar.load(state.info.avatarUrl) {
                                        crossfade(true)
                                        transformations(CircleCropTransformation())
                                    }
                                    headerUsername.text = state.info.username
                                }
                            } else {
                                initHeaderView()
                            }
                        } else {
                            headerAvatar.load(R.mipmap.ic_launcher) {
                                crossfade(true)
                                transformations(CircleCropTransformation())
                            }
                            headerUsername.setText(R.string.loading)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (slideOffset > 0f) {
                binding.fcvMain.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        6.dp * slideOffset,
                        6.dp * slideOffset,
                        Shader.TileMode.CLAMP
                    )
                )
            }
        }
    }

    override fun onDrawerClosed(drawerView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.fcvMain.setRenderEffect(null)
        }
    }

    override fun onDrawerOpened(drawerView: View) {

    }

    override fun onDrawerStateChanged(newState: Int) {

    }

    private fun showFindRelatedLinkSnackBar(videoCode: String) {
        showSnackBar(R.string.detect_ha1_related_link_in_clipboard, Snackbar.LENGTH_LONG) {
            setAction(R.string.enter) {
                startActivity<VideoActivity>(VIDEO_CODE to videoCode)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initHeaderView() {
        binding.nvMain.getHeaderView(0)?.let { view ->
            val headerAvatar = view.findViewById<ImageView>(R.id.header_avatar)
            val headerUsername = view.findViewById<TextView>(R.id.header_username)
            if (isAlreadyLogin) {
                headerAvatar.setOnClickListener {
                    showAlertDialog {
                        setTitle(R.string.sure_to_logout)
                        setPositiveButton(R.string.sure) { _, _ ->
                            logoutWithRefresh()
                        }
                        setNegativeButton(R.string.no, null)
                    }
                }
            } else {
                headerAvatar.load(R.drawable.neuro) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                headerUsername.setText(R.string.not_logged_in)
                headerAvatar.setOnClickListener {
                    gotoLoginActivity()
                }
            }
        }
    }

    // #issue-225: 侧滑选单双重点击异常，不能从 xml 里直接定义 activity 块，需要在代码里初始化
//    private fun initNavActivity() {
//        binding.nvMain.menu.apply {
//            findItem(R.id.nv_settings).setOnMenuItemClickListener {
//                SettingsRouter.with(navController).toSettingsActivity()
//                return@setOnMenuItemClickListener false
//            }
//            findItem(R.id.nv_h_keyframe_settings).setOnMenuItemClickListener {
//                SettingsRouter.with(navController)
//                    .toSettingsActivity(R.id.hKeyframeSettingsFragment)
//                return@setOnMenuItemClickListener false
//            }
//            findItem(R.id.nv_download).setOnMenuItemClickListener {
//                startActivity<DownloadActivity>()
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//                    @Suppress("DEPRECATION")
//                    overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.fade_out)
//                }
//                return@setOnMenuItemClickListener false
//            }
//        }
//    }

    private val loginNeededFragmentList =
        intArrayOf(R.id.nv_fav_video, R.id.nv_watch_later, R.id.nv_playlist)

    private fun initMenu() {
        if (isAlreadyLogin) {
            loginNeededFragmentList.forEach {
                binding.nvMain.menu.findItem(it).setOnMenuItemClickListener(null)
            }
        } else {
            loginNeededFragmentList.forEach {
                binding.nvMain.menu.findItem(it).setOnMenuItemClickListener {
                    showShortToast(R.string.login_first)
                    return@setOnMenuItemClickListener false
                }
            }
        }
    }

    private fun gotoLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        loginDataLauncher.launch(intent)
    }

    private fun logoutWithRefresh() {
        logout()
        initHeaderView()
        initMenu()
    }

    /**
     * 设置toolbar与navController关联
     *
     * 必须最后调用！先设置好toolbar！
     */
    fun Toolbar.setupWithMainNavController() {
        supportActionBar!!.title = hanimeSpannedTitle
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nv_home_page), binding.dlMain)
        this.setupWithNavController(navController, appBarConfiguration)
    }

    override fun setupToolbar(title: CharSequence, canNavigateBack: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(canNavigateBack)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun hideToolbar() {
        binding.toolbar.visibility = View.GONE
    }

    override fun showToolbar() {
        binding.toolbar.visibility = View.VISIBLE
    }

    fun showVideoDetailFragment(videoCode: String) {

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv_main)
        val childFragmentManager = navHostFragment?.childFragmentManager
        if (childFragmentManager != null && !childFragmentManager.isStateSaved) {
            val navController = navHostFragment.findNavController()
            val args = bundleOf(VIDEO_CODE to videoCode)
            val options = navOptions {
                anim {
                    enter = R.anim.fade_in
                    exit = R.anim.fade_out
                    popEnter = R.anim.fade_in
                    popExit = R.anim.fade_out
                }
                launchSingleTop = true
            }
            navController.navigate(R.id.video_vp, args, options)
        } else {
            Log.w("Navigation", "❌ Cannot navigate: FragmentManager has already saved its state.")
        }
    }

    enum class LayoutMode {
        NAV_LEFT, NAV_RIGHT, SINGLE_COLUMN
    }

    private var currentMode = LayoutMode.NAV_LEFT
    fun swapFragments(number: Number) {
        val nav = findViewById<NavigationView>(R.id.nv_main)
        val content = findViewById<CoordinatorLayout>(R.id.right_pan)

        val parent = findViewById<LinearLayout>(R.id.main_layout)
        TransitionManager.beginDelayedTransition(parent)

        when (currentMode) {
            LayoutMode.NAV_LEFT -> {
                // 切换到右导航（先调整顺序）
                parent.removeView(nav)
                parent.removeView(content)

//                nav.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
//                content.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f)
                parent.addView(content)
                parent.addView(nav)
                currentMode = LayoutMode.NAV_RIGHT
                Toast.makeText(this, "$currentMode", Toast.LENGTH_SHORT).show()
            }

            LayoutMode.NAV_RIGHT -> {

                nav.visibility = View.VISIBLE

                parent.removeView(nav)
                parent.removeView(content)
                parent.addView(nav)
                parent.addView(content)
                currentMode = LayoutMode.NAV_LEFT

                Toast.makeText(this, "$currentMode", Toast.LENGTH_SHORT).show()
            }

            LayoutMode.SINGLE_COLUMN -> {

//                nav.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
//                content.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3f)
                nav.visibility = View.GONE
                content.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                currentMode = LayoutMode.NAV_LEFT
                Toast.makeText(this, "$currentMode", Toast.LENGTH_SHORT).show()
            }
        }
    }


}