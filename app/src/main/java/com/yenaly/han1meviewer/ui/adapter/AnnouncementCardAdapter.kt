package com.yenaly.han1meviewer.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemAnnouncementCardBinding
import com.yenaly.han1meviewer.logic.model.Announcement

class AnnouncementCardAdapter(
    private val items: List<Announcement>,
    private val onClick: (Announcement) -> Unit,
    private val onClose: () -> Unit
) : RecyclerView.Adapter<AnnouncementCardAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemAnnouncementCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val adapter = AnnouncementAdapter(items, onClick)
            binding.announcementPager.adapter = adapter
            val indicatorLayout = binding.indicatorLayout
            setupIndicators(indicatorLayout, items.size)
            setCurrentIndicator(indicatorLayout, 0)

            binding.announcementPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    setCurrentIndicator(indicatorLayout, position)
                }
            })

            binding.btnClose.setOnClickListener {
                onClose()
            }
        }

        private fun setupIndicators(indicatorLayout: LinearLayout, count: Int) {
            indicatorLayout.removeAllViews()
            repeat(count) {
                val indicator = ImageView(indicatorLayout.context).apply {
                    setImageResource(R.drawable.indicator_inactive)
                    val params = LinearLayout.LayoutParams(20, 20)
                    params.setMargins(8, 0, 8, 0)
                    layoutParams = params
                }
                indicatorLayout.addView(indicator)
            }
        }

        private fun setCurrentIndicator(indicatorLayout: LinearLayout, index: Int) {
            for (i in 0 until indicatorLayout.childCount) {
                val imageView = indicatorLayout.getChildAt(i) as? ImageView ?: continue
                imageView.setImageResource(
                    if (i == index) R.drawable.indicator_active
                    else R.drawable.indicator_inactive
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }
}
