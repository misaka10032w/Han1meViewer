package com.yenaly.han1meviewer.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.HanimeConstants.ANIME_URL
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.databinding.ActivityMainBinding
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.logout
import com.yenaly.han1meviewer.ui.fragment.PermissionRequester
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.fragment.settings.NetworkSettingsFragment
import com.yenaly.han1meviewer.ui.fragment.video.VideoFragment
import com.yenaly.han1meviewer.ui.screen.main.MainDrawerHeader
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.logScreenViewEvent
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.showUpdateDialog
import com.yenaly.han1meviewer.videoUrlRegex
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.base.YenalyActivity
import com.yenaly.yenaly_libs.utils.dp
import com.yenaly.yenaly_libs.utils.showShortToast
import com.yenaly.yenaly_libs.utils.showSnackBar
import com.yenaly.yenaly_libs.utils.textFromClipboard
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import androidx.core.graphics.createBitmap

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:35
 */
class MainActivity : YenalyActivity<ActivityMainBinding>(), DrawerListener, ToolbarHost,
    PermissionRequester {

    val viewModel by viewModels<MainViewModel>()

    private lateinit var navHostFragment: NavHostFragment
    lateinit var navController: NavController
//    private var detailNavController: NavController? = null

    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1234
        const val ACTION_TOGGLE_PLAY = "com.yenaly.han1meviewer.ACTION_TOGGLE_PLAY"
    }

    val currentFragment get() = navHostFragment.childFragmentManager.primaryNavigationFragment

    // 登錄完了後讓activity刷新主頁
    private val loginDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.getHomePage()
                initHeaderView()
                initMenu()
            }
        }
    private var hasAuthenticated = false
    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("pipmode", "✅ onReceive called with action: ${intent?.action}")
            when (intent?.action) {
                ACTION_TOGGLE_PLAY -> {
                    Log.i("pipmode", "🎬 ACTION_TOGGLE_PLAY triggered")
                    togglePlayPause()
                }
            }
        }
    }

    // Predictive back preview: cache of previous fragment for showing behind scaled view
    private var headerAvatarUrl by mutableStateOf<String?>(null)
    private var headerUsername by mutableStateOf<String?>(null)
    private var headerIsLoggedIn by mutableStateOf(false)
    private var headerIsLoading by mutableStateOf(false)
    private var headerCurrentSite by mutableStateOf(Preferences.baseUrl)
    private var previewImageView: ImageView? = null
    private val previewBitmapCache = HashMap<Int, android.graphics.Bitmap>() // destId → bitmap
    private val backCornerRadius by lazy { 24f.dp.toFloat() }


    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    override fun setUiStyle() {
        enableEdgeToEdge()
    }

    override val onFragmentResumedListener: (Fragment) -> Unit = { fragment ->
        logScreenViewEvent(fragment)
    }

    /**
     * 初始化数据
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun initData(savedInstanceState: Bundle?) {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv_main) as NavHostFragment
        navController = navHostFragment.navController
        binding.dlMain.addDrawerListener(this)
        binding.nvMain.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
            true
        }
        setSupportActionBar(findViewById(R.id.toolbar))
        initHeaderView()
        initMenu()
        
        // Initialize preview ImageView for predictive back gesture
        previewImageView = binding.root.findViewById(R.id.preview_image)
        previewImageView?.isClickable = false
        previewImageView?.isEnabled = false
        previewImageView?.setOnTouchListener { _, _ -> false }

        // Cache fragment views when they become primary for predictive back preview
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    val destId = navController.currentDestination?.id ?: return
                    val view = f.view ?: return
                    if (view.width <= 0 || view.height <= 0) return
                    try {
                        val bmp = createBitmap(view.width, view.height)
                        val canvas = android.graphics.Canvas(bmp)
                        view.draw(canvas)
                        synchronized(previewBitmapCache) {
                            previewBitmapCache[destId]?.recycle()
                            previewBitmapCache[destId] = bmp
                        }
                    } catch (_: Exception) {}
                }
            },
            false
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.nvMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentCheckedId = binding.nvMain.checkedItem?.itemId
            val targetId = when (destination.id) {
                R.id.nv_home_page -> R.id.nv_home_page
                R.id.nv_watch_history -> R.id.nv_watch_history
                R.id.nv_fav_video -> R.id.nv_fav_video
                R.id.nv_playlist -> R.id.nv_playlist
                R.id.nv_watch_later -> R.id.nv_watch_later
                R.id.nav_settings -> R.id.nv_settings
                R.id.nv_subscription -> R.id.nv_subscription
                R.id.nv_download -> R.id.nv_download
                else -> null
            }
            if (targetId != null && targetId != currentCheckedId) {
                binding.nvMain.setCheckedItem(targetId)
            }
            if (destination.id == R.id.nv_home_page){
                binding.dlMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                binding.dlMain.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
        setupPredictiveBack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("UNUSED_VARIABLE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen().apply {
                setKeepOnScreenCondition { !hasAuthenticated }
            }
        } else null
        super.onCreate(savedInstanceState)
//        window.decorView.post { setStatusBarIcons() }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val useLock = prefs.getBoolean("use_lock_screen", false)

        if (useLock && isDeviceSecureCompat(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                authenticate(
                    this,
                    onSuccess = {
                        removeAuthGuard()
                        hasAuthenticated = true
                        initData(savedInstanceState)
                    },
                    onFailed = {
                        finish()
                    }
                )
            } else {
                // Android 7~8，不支持 BiometricPrompt
                Toast.makeText(this, R.string.not_compact_lock_screen, Toast.LENGTH_SHORT).show()
                removeAuthGuard()
                hasAuthenticated = true
                initData(savedInstanceState)
            }
        } else {
            removeAuthGuard()
            hasAuthenticated = true
            initData(savedInstanceState)
        }
        handleDeeplinkIfNeeded(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeeplinkIfNeeded(intent)
    }

    private fun handleDeeplinkIfNeeded(intent: Intent) {
        Log.i("deeplink", "intent=$intent")

        //外部跳转Deeplink
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            when (uri.scheme) {
                "http", "https" -> {
                    val videoCode = uri.getQueryParameter("v")
                    if (videoCode != null) {
                        showVideoDetailFragment(videoCode)
                    }
                }
                "file", "content" -> {
                    showVideoDetailFragment("-1",uri.toString())
                }
            }
            return
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcv_main) as? NavHostFragment ?: return
        val navController = navHostFragment.navController
        if (navHostFragment.childFragmentManager.isStateSaved) {
            Log.w("deeplink", "❌ Cannot navigate: state already saved")
            return
        }

        //String形式TAG
        intent.getStringExtra("startSearchFromTag")?.let { tag ->
            intent.removeExtra("startSearchFromTag")
            if (navController.currentDestination?.id != R.id.searchFragment) {
                navController.navigate(
                    R.id.searchFragment,
                    bundleOf(ADVANCED_SEARCH_MAP to tag)
                )
            }
            return
        }

        // Map形式TAG
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
        if (map != null) {
            intent.removeExtra("startSearchFromMap")
            navController.navigate(
                R.id.searchFragment,
                bundleOf(ADVANCED_SEARCH_MAP to map)
            )
        }

        val videoCode = intent.getStringExtra("startVideoCode")
        if (!videoCode.isNullOrEmpty()) {
            intent.removeExtra("startVideoCode")
            showVideoDetailFragment(videoCode)
            return
        }

    }

    private fun removeAuthGuard() {
        val root = findViewById<ViewGroup>(R.id.dl_main)
        val authGuard = findViewById<View>(R.id.auth_guard)
        root?.removeView(authGuard)
    }

    private fun isDeviceSecureCompat(context: Context): Boolean {
        val km = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    private fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    // 指纹被识别但不匹配（单次）
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 取消、锁定、连续失败后触发
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_request))
            .setSubtitle(getString(R.string.unlock_method))
            .setDescription(getString(R.string.unlock_desc))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
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
            R.id.nv_home_page -> safeNavigateTo(R.id.nv_home_page)
            R.id.nv_watch_history -> safeNavigateTo(R.id.action_nv_home_page_to_nv_watch_history)
            R.id.nv_fav_video -> safeNavigateTo(R.id.action_nv_home_page_to_nv_fav_video)
            R.id.nv_playlist -> safeNavigateTo(R.id.action_nv_home_page_to_myPlayListFragmentV2)
            R.id.nv_watch_later -> safeNavigateTo(R.id.action_nv_home_page_to_nv_watch_later)
            R.id.nv_subscription -> safeNavigateTo(R.id.action_nv_home_page_to_nv_subscription)
            R.id.nv_daily_check_in -> safeNavigateTo(R.id.action_nv_home_page_to_nv_daily_check_in)
            R.id.nv_download -> safeNavigateTo(R.id.action_nv_home_page_to_nv_download)

            // 设置相关 - 这些在 nav_settings.xml 中
            R.id.nv_settings -> {
                // 导航到设置图的起始目的地
                navController.navigate(R.id.action_global_nav_settings)
            }
        }
        binding.dlMain.closeDrawer(GravityCompat.START)
    }

    private fun safeNavigateTo(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: IllegalArgumentException) {
            Log.e("Navigation", "Navigation destination not found: $destinationId", e)
        }
    }

    override fun onStart() {
        super.onStart()
        registerPipReceiver()
        binding.root.post {
            textFromClipboard?.let {
                videoUrlRegex.find(it)?.groupValues?.get(1)?.let { videoCode ->
                    showFindRelatedLinkSnackBar(videoCode)
                }
            }
        }
    }

    private fun registerPipReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE_PLAY)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipActionReceiver, filter, RECEIVER_NOT_EXPORTED)
            Log.i("pipmode", "✅ registerReceiver with RECEIVER_NOT_EXPORTED")
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(pipActionReceiver, filter)
            Log.i("pipmode", "✅ registerReceiver (legacy)")
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(pipActionReceiver)
    }

    @OptIn(ExperimentalTime::class)
    override fun bindDataObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                AppViewModel.versionFlow.collect { state ->
                    if (state is WebsiteState.Success && Preferences.isUpdateDialogVisible) {
                        state.info?.let { release ->
                            Preferences.lastUpdatePopupTime =
                                kotlin.time.Clock.System.now().epochSeconds
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.homePageFlow.collect { state ->
                    if (state is WebsiteState.Success) {
                        headerIsLoading = false
                        if (isAlreadyLogin) {
                            headerIsLoggedIn = true
                            headerAvatarUrl = state.info.avatarUrl
                            headerUsername = state.info.username
                        } else {
                            initHeaderView()
                        }
                    } else {
                        headerIsLoading = true
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
                showVideoDetailFragment(videoCode)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initHeaderView() {
        headerCurrentSite = Preferences.baseUrl
        headerIsLoggedIn = isAlreadyLogin
        headerIsLoading = !isAlreadyLogin
        binding.nvMain.getHeaderView(0)?.let { view ->
            (view.findViewById<ComposeView>(R.id.drawer_header_compose))?.setContent {
                HanimeTheme {
                    MainDrawerHeader(
                        avatarUrl = headerAvatarUrl,
                        username = headerUsername,
                        isLoggedIn = headerIsLoggedIn,
                        isLoading = headerIsLoading,
                        currentSite = headerCurrentSite,
                        onAvatarClick = {
                            if (isAlreadyLogin) {
                                showAlertDialog {
                                    setTitle(R.string.sure_to_logout)
                                    setPositiveButton(R.string.sure) { _, _ -> logoutWithRefresh() }
                                    setNegativeButton(R.string.no, null)
                                }
                            } else {
                                gotoLoginActivity()
                            }
                        },
                        onSwitchSiteClick = { showSiteSwitchDialog() },
                    )
                }
            }
        }
    }

    private fun showSiteSwitchDialog() {
        val currentSite = Preferences.baseUrl
        showAlertDialog {
            setTitle(R.string.confirm_switch_site)
            setPositiveButton(R.string.sure) { _, _ ->
                val avSite = HANIME_URL[3]
                val selectedBaseUrl = Preferences.selectedBaseUrl
                if (currentSite in ANIME_URL) {
                    Preferences.preferenceSp.edit(true) {
                        putString(NetworkSettingsFragment.SELECTED_BASE_URL, currentSite)
                        putString(NetworkSettingsFragment.DOMAIN_NAME, avSite)
                    }
                } else {
                    Preferences.preferenceSp.edit(true) {
                        putString(NetworkSettingsFragment.SELECTED_BASE_URL, selectedBaseUrl)
                        putString(NetworkSettingsFragment.DOMAIN_NAME, selectedBaseUrl)
                    }
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    ActivityManager.restart(killProcess = true)
                }, 500)
            }
            setNegativeButton(R.string.no, null)
        }
    }

    private val loginNeededFragmentList =
        intArrayOf(R.id.nv_fav_video, R.id.nv_watch_later, R.id.nv_playlist, R.id.nv_subscription)

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
        supportActionBar!!.title = if (Preferences.baseUrl == HANIME_URL[3]) createAppbarTitleAV(context) else createAppbarTitle(context)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nv_home_page), binding.dlMain)
        this.setupWithNavController(navController, appBarConfiguration)

        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            (this.navigationIcon as? androidx.appcompat.graphics.drawable.DrawerArrowDrawable)?.color = typedValue.data
        }
    }

    fun createAppbarTitle(context: Context): SpannableString {
        val fullText = "Han1meViewer"
        val spannable = SpannableString(fullText)

        val redSpan = object : CharacterStyle() {
            private val color = Color.RED
            override fun updateDrawState(tp: TextPaint) {
                tp.color = color
                tp.isFakeBoldText = true
            }
        }
        spannable.setSpan(redSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true)
        val themeColor = typedValue.data

        val boldThemeSpan = object : CharacterStyle() {
            override fun updateDrawState(tp: TextPaint) {
                tp.color = themeColor
                tp.isFakeBoldText = true
            }
        }

        spannable.setSpan(boldThemeSpan, 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val normalThemeSpan = object : CharacterStyle() {
            override fun updateDrawState(tp: TextPaint) {
                tp.color = themeColor
                tp.isFakeBoldText = false
            }
        }

        spannable.setSpan(normalThemeSpan, 6, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    fun createAppbarTitleAV(context: Context): SpannableString {
        val fullText = "HAViewer"
        val spannable = SpannableString(fullText)

        val redSpan = object : CharacterStyle() {
            private val color = Color.RED
            override fun updateDrawState(tp: TextPaint) {
                tp.color = color
                tp.isFakeBoldText = true
            }
        }
        spannable.setSpan(redSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true)
        val themeColor = typedValue.data

        val boldThemeSpan = object : CharacterStyle() {
            override fun updateDrawState(tp: TextPaint) {
                tp.color = themeColor
                tp.isFakeBoldText = true
            }
        }

        spannable.setSpan(boldThemeSpan, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val normalThemeSpan = object : CharacterStyle() {
            override fun updateDrawState(tp: TextPaint) {
                tp.color = themeColor
                tp.isFakeBoldText = false
            }
        }

        spannable.setSpan(normalThemeSpan, 2, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }


    override fun setupToolbar(title: CharSequence, canNavigateBack: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.menu.clear()
        supportActionBar?.apply {
            this.title = title
            setDisplayHomeAsUpEnabled(canNavigateBack)
            if (canNavigateBack) {
                setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24)
            } else {
                setHomeAsUpIndicator(null)
            }
        }
    }

    override fun hideToolbar() {
        binding.toolbar.visibility = View.GONE
    }

    override fun showToolbar() {
        binding.toolbar.visibility = View.VISIBLE
    }

    fun showVideoDetailFragment(videoCode: String, fileUri: String? = null) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv_main)
        val childFragmentManager = navHostFragment?.childFragmentManager

        if (childFragmentManager != null && !childFragmentManager.isStateSaved) {
//            val navController = navHostFragment.findNavController()
            val args = bundleOf(
                VIDEO_CODE to videoCode,
                "LOCAL_URI" to fileUri
            ) // KEY 要与 Fragment 中读取的 key 对应
            navController.navigate(
                R.id.videoFragment,
                args
            )
        } else {
            Log.w("Navigation", "❌ Cannot navigate: FragmentManager has already saved its state.")
        }
    }

    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    private var onPermanentlyDenied: (() -> Unit)? = null
    override fun requestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                this.onGranted = onGranted
                this.onDenied = onDenied
                this.onPermanentlyDenied = onPermanentlyDenied
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        } else {
            onGranted() // Android 10+ 不需要权限
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            val permission = permissions.getOrNull(0)
            val grantResult = grantResults.getOrNull(0)

            if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                when {
                    grantResult == PackageManager.PERMISSION_GRANTED -> {
                        onGranted?.invoke()
                    }

                    shouldShowRequestPermissionRationale(permission) -> {
                        onDenied?.invoke()
                    }

                    else -> {
                        // 永久拒绝（勾选“不再询问”）
                        onPermanentlyDenied?.invoke()
                    }
                }
                // 清除引用，防止内存泄露
                onGranted = null
                onDenied = null
                onPermanentlyDenied = null
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.fcv_main)
            ?.childFragmentManager
            ?.primaryNavigationFragment

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val allowPip = prefs.getBoolean("allow_pip_mode", true)

        Log.i("pipmode", "enter pip mode?\n$currentFragment\nallowpip:$allowPip\n")

        if (currentFragment is VideoFragment && currentFragment.shouldEnterPip() && allowPip) {
            Log.i("pipmode", "enter pip mode")
            currentFragment.enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.fcv_main)
            ?.childFragmentManager
            ?.primaryNavigationFragment

        if (currentFragment is VideoFragment) {
            currentFragment.onPipModeChanged(isInPictureInPictureMode)
        }
    }

    fun togglePlayPause() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fcv_main) as NavHostFragment
        val videoFragment =
            navHostFragment.childFragmentManager.primaryNavigationFragment as? VideoFragment
        videoFragment?.togglePlayPause()
    }

    private fun setupPredictiveBack() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (Preferences.disablePredictiveBack) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY
            ) {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                object : android.window.OnBackAnimationCallback {
                    private var fragmentView: View? = null
                    private var viewWidth = 0f
                    private var viewHeight = 0f

                    override fun onBackStarted(backEvent: android.window.BackEvent) {
                        if (navController.currentDestination?.id == R.id.nv_home_page) {
                            fragmentView = null
                            return
                        }
                        fragmentView = navHostFragment.childFragmentManager
                            .primaryNavigationFragment?.view
                        fragmentView?.let {
                            viewWidth = it.width.toFloat()
                            viewHeight = it.height.toFloat()

                            // Apply rounded corners
                            it.clipToOutline = true
                            it.outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(
                                        0, 0, view.width, view.height, backCornerRadius
                                    )
                                }
                            }

                            // Show the cached preview of previous fragment
                            showCachedPreview()
                        }
                    }

                    override fun onBackProgressed(backEvent: android.window.BackEvent) {
                        val v = fragmentView ?: return
                        val progress = backEvent.progress.coerceIn(0f, 1f)
                        val scale = 1f - progress * 0.1f
                        val touchX = backEvent.touchX
                        val touchY = backEvent.touchY
                        v.pivotX = viewWidth / 2f
                        v.pivotY = viewHeight / 2f
                        v.scaleX = scale
                        v.scaleY = scale
                        v.translationX = (touchX - viewWidth / 2f) * progress * 0.1f
                        v.translationY = (touchY - viewHeight / 2f) * progress * 0.1f
                    }

                    override fun onBackInvoked() {
                        fragmentView?.clipToOutline = false
                        hidePreview()
                        onBackPressedDispatcher.onBackPressed()
                    }

                    override fun onBackCancelled() {
                        fragmentView?.clipToOutline = false
                        hidePreview()
                        fragmentView?.animate()
                            ?.scaleX(1f)?.scaleY(1f)
                            ?.translationX(0f)?.translationY(0f)
                            ?.setDuration(200)
                            ?.start()
                    }
                }
            )
        }
    }

    /**
     * Show the cached preview bitmap when back gesture starts
     */
    private fun showCachedPreview() {
        val prevDestId = navController.previousBackStackEntry?.destination?.id ?: return
        val bitmap = synchronized(previewBitmapCache) {
            previewBitmapCache[prevDestId]
        }
        if (bitmap != null && previewImageView != null) {
            previewImageView?.setImageBitmap(bitmap)
            previewImageView?.visibility = View.VISIBLE
        }
    }

    /**
     * Hide the preview ImageView and dim overlay
     */
    private fun hidePreview() {
        previewImageView?.visibility = View.GONE
        previewImageView?.setImageBitmap(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronized(previewBitmapCache) {
            previewBitmapCache.values.forEach { it.recycle() }
            previewBitmapCache.clear()
        }
    }

}