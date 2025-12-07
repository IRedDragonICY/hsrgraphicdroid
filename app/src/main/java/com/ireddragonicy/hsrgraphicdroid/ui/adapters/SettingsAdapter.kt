package com.ireddragonicy.hsrgraphicdroid.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ireddragonicy.hsrgraphicdroid.databinding.ItemSettingBinding

data class SettingItem(
    val id: SettingId,
    val title: String,
    val summary: String,
    val iconRes: Int,
    val type: SettingType,
    val isChecked: Boolean = false
)

enum class SettingId {
    THEME,
    LANGUAGE,
    DYNAMIC_COLOR,
    ABOUT
}

enum class SettingType {
    SWITCH,
    NAVIGATION
}

class SettingsAdapter(
    private val onItemClick: (SettingItem) -> Unit,
    private val onToggleChanged: (SettingItem, Boolean) -> Unit
) : ListAdapter<SettingItem, SettingsAdapter.ViewHolder>(SettingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SettingItem) {
            binding.ivIcon.setImageResource(item.iconRes)
            binding.tvTitle.text = item.title
            binding.tvSummary.text = item.summary

            binding.switchAction.visibility = View.GONE
            binding.ivChevron.visibility = View.GONE

            when (item.type) {
                SettingType.SWITCH -> {
                    binding.switchAction.visibility = View.VISIBLE
                    binding.switchAction.isChecked = item.isChecked
                    binding.switchAction.setOnCheckedChangeListener { _, checked ->
                        onToggleChanged(item, checked)
                    }
                    binding.root.setOnClickListener {
                        binding.switchAction.toggle()
                    }
                }

                SettingType.NAVIGATION -> {
                    binding.ivChevron.visibility = View.VISIBLE
                    binding.root.setOnClickListener { onItemClick(item) }
                }
            }
        }
    }

    private class SettingDiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean =
            oldItem == newItem
    }
}

