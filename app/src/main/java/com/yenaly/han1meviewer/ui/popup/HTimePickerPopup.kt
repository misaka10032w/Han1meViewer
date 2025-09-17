package com.yenaly.han1meviewer.ui.popup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ViewFlipper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout
import com.lxj.xpopupext.popup.TimePickerPopup
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.ui.adapter.ReleaseDateAdapter

/**
 * #issue-161: 高级搜索可以选择年或年月
 */
class HTimePickerPopup(
    context: Context,
    private val timeList: List<SearchOption>,
    private val onItemSelected: (SearchOption) -> Unit
) : TimePickerPopup(context) {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, emptyList(), {})

    private lateinit var btnSwitch: MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var viewFlipper: ViewFlipper

    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton
    private val btnTextColor by lazy {
        MaterialColors.getColor(
            rootView,
            com.google.android.material.R.attr.colorOnPrimary)
    }

    var mode: Mode = Mode.YM
        private set

    override fun getImplLayoutId(): Int = R.layout.pop_up_ext_h_time_picker

    override fun onCreate() {
        super.onCreate()
        btnSwitch = findViewById(R.id.btnSwitch)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        btnCancel.setTextColor(btnTextColor)
        btnConfirm.setTextColor(btnTextColor)

        btnSwitch.text = when (mode) {
            Mode.YM -> context.getString(R.string.switch_to_year)
            else -> context.getString(R.string.switch_to_year_month)
        }
        btnSwitch.setOnClickListener {
            when (mode) {
                Mode.YM -> setMode(Mode.Y)
                else -> setMode(Mode.YM)
            }
            onCreate()
        }
        val recyclerView = findViewById<RecyclerView>(R.id.releaseDateRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ReleaseDateAdapter(timeList) { selected ->
            onItemSelected(selected)
            dismiss()
        }
        initTabView()
        popupImplView.background = null //清除TimePickerPopup自带的背景
    }

    override fun getPopupImplView(): View {
        return findViewById(R.id.popup_root)
    }

    private fun initTabView() {
        tabLayout = findViewById(R.id.timePickerTabLayout)
        viewFlipper = findViewById(R.id.viewFlipper)
        tabLayout.removeAllTabs()
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.specific_y_m)))
        tabLayout.addTab(tabLayout.newTab().setText(context.getString(R.string.approximate_range)))
        var lastSelectedTab = 0

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val newPosition = tab.position
                if (newPosition > lastSelectedTab) {
                    viewFlipper.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
                    viewFlipper.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)
                } else {
                    viewFlipper.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_left)
                    viewFlipper.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
                }
                viewFlipper.displayedChild = newPosition
                lastSelectedTab = newPosition
                btnSwitch.visibility = if (newPosition == 0) VISIBLE else GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        viewFlipper.displayedChild = 0
        btnSwitch.visibility = VISIBLE
    }

    override fun setMode(mode: Mode): TimePickerPopup {
        this.mode = mode
        return super.setMode(mode)
    }
}