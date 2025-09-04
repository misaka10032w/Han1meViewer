package com.yenaly.han1meviewer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.adapter.HanimeVideoRvAdapter
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import kotlin.math.max

class PlaylistBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "PlaylistBottomSheetFragment"
    }

    private val viewModel: VideoViewModel by viewModels({ requireParentFragment() })
    private var videoCount = 0
    private lateinit var adapter: HanimeVideoRvAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bottom_sheet_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = HanimeVideoRvAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_vertical_list)
        val countText = view.findViewById<TextView>(R.id.video_count)
        recyclerView.adapter = adapter
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener{
                override fun onGlobalLayout() {
                    recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val spanCount = calculateSpanCount(recyclerView,185)
                    recyclerView.layoutManager = GridLayoutManager(requireContext(),spanCount)
                }
            }
        )

        viewModel.videoList.observe(viewLifecycleOwner) { list ->
            videoCount = list.size
            countText.text = getString(R.string.blank_brackets,videoCount)
            adapter.submitList(list)
        }

        recyclerView.addOnLayoutChangeListener {  _, left, _, right, _, _, _, _, _ ->
            val newWidth = right - left
            if (newWidth > 0) {
                val spanCount = calculateSpanCount(
                    recyclerView,
                    requireContext().resources
                        .getDimension(R.dimen.video_cover_width)
                        .toInt()
                )
                (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = spanCount
            }
        }
    }
    private fun calculateSpanCount(recyclerView: RecyclerView, itemMinWidthDp: Int): Int {
        val density = recyclerView.resources.displayMetrics.density
        val itemMinWidthPx = (itemMinWidthDp * density).toInt()
        val totalSpace = recyclerView.measuredWidth - recyclerView.paddingLeft - recyclerView.paddingRight
        return max(2, totalSpace / itemMinWidthPx)
    }
}