package com.yenaly.han1meviewer.ui.fragment.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.yenaly.han1meviewer.ADVANCED_SEARCH_MAP
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.fragment.ToolbarHost
import com.yenaly.han1meviewer.ui.fragment.funny.FunnyTouchListener
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import com.yenaly.han1meviewer.ui.viewmodel.MainViewModel
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.openVideo
import com.yenaly.yenaly_libs.utils.application
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.showShortToast
import coil.load
import com.yenaly.han1meviewer.ui.screen.home.HomePageScreen
import com.yenaly.han1meviewer.ui.screen.home.LocalSearchHistoryQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.Serializable

/**
 * 首页 Fragment - Compose 化改造版本
 */
class HomePageFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val checkInViewModel: CheckInCalendarViewModel by activityViewModels()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val someFunnyTouchListener by lazy {
        FunnyTouchListener(application) { showShortToast("WTF?") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBadGuy(requireContext(), R.raw.akarin)
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { showExitConfirmationDialog() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HanimeTheme {
                    CompositionLocalProvider(
                        LocalSearchHistoryQuery provides { keyword ->
                            withContext(Dispatchers.IO) {
                                DatabaseRepo.SearchHistory.loadAll(keyword).first().map { it.query }
                            }
                        }
                    ) {
                        HomePageScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { openDrawer() },
                            onNavigateToPreview = { navigateToPreview() },
                            onNavigateToSearch = { query -> navigateToSearch(query) },
                            onOpenSearchPage = { navigateToSearchPage() },
                            onNavigateToSearchAdvanced = { params -> navigateToSearchAdvanced(params) },
                            onOpenVideo = { code -> openVideo(code) },
                            onLongPressVideoCopy = { code, title ->
                                copyTextToClipboard(getHanimeShareText(title, code))
                                showShortToast(R.string.copy_to_clipboard)
                            },
                            onShowExitDialog = { showExitConfirmationDialog() },
                            onShowAnnouncementDialog = { title, content, imageUrl ->
                                showAnnouncementDialog(
                                    requireContext(),
                                    title,
                                    SpannableString(content),
                                    imageUrl
                                )
                            }
                        )
                    }
                }
            }
            setOnTouchListener(someFunnyTouchListener)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            onBackPressedCallback.isEnabled = true
            view?.post { (activity as? ToolbarHost)?.hideToolbar() }
        }
    }

    override fun onPause() {
        super.onPause()
        onBackPressedCallback.isEnabled = false
        (activity as? MainActivity)?.setSupportActionBar(activity?.findViewById(R.id.toolbar))
    }

    private fun openDrawer() {
        (activity as? MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.dl_main)
            ?.openDrawer(GravityCompat.START)
    }

    private fun navigateToPreview() {
        if (findNavController().currentDestination?.id == R.id.nv_home_page)
            findNavController().navigate(R.id.action_nv_home_page_to_nv_preview)
    }

    private fun navigateToSearch(query: String) {
        findNavController().navigate(R.id.searchFragment, android.os.Bundle().apply {
            putSerializable(ADVANCED_SEARCH_MAP, query)
        })
    }

    /** 直接打开搜索页（不传查询词） */
    private fun navigateToSearchPage() {
        findNavController().navigate(R.id.searchFragment)
    }

    private fun navigateToSearchAdvanced(params: Map<String, String>) {
        val m = HashMap<String, Serializable>(params.mapValues { it.value })
        findNavController().navigate(R.id.searchFragment, android.os.Bundle().apply {
            putSerializable(ADVANCED_SEARCH_MAP, m)
        })
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_to_exit))
            .setMessage(getString(R.string.finished_masturbating))
            .setNegativeButton(getString(R.string.do_more)) { d, _ -> d.dismiss() }
            .setNeutralButton(getString(R.string.checkout_exit)) { _, _ ->
                checkInViewModel.addRecord(java.time.LocalDate.now(), java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), "自慰", "", "")
                requireActivity().finish()
            }
            .setPositiveButton(getString(R.string.exit)) { _, _ -> requireActivity().finish() }
            .show()
    }

    private fun showAnnouncementDialog(ctx: Context, title: String, content: Spanned, imageUrl: String?) {
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_announcement, null, false)
        v.findViewById<TextView>(R.id.dialogTitle).apply { text = title; visibility = View.VISIBLE }
        v.findViewById<TextView>(R.id.dialogContent).apply { text = content; movementMethod = LinkMovementMethod.getInstance(); highlightColor = Color.TRANSPARENT }
        if (!imageUrl.isNullOrBlank()) {
            v.findViewById<ShapeableImageView>(R.id.dialogImage).apply {
                visibility = View.VISIBLE
                load(imageUrl) { placeholder(R.drawable.akarin); error(R.drawable.baseline_error_outline_24) }
                setOnClickListener {
                    Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                        setContentView(ImageView(ctx).apply { layoutParams = ViewGroup.LayoutParams(-1, -1); scaleType = ImageView.ScaleType.FIT_CENTER; load(imageUrl); setOnClickListener { dismiss() } })
                        show()
                    }
                }
            }
        }
        MaterialAlertDialogBuilder(ctx).setView(v).setPositiveButton(getString(R.string.i_understand), null).show()
    }
}
