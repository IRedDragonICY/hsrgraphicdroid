package com.ireddragonicy.hsrgraphicdroid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.ItemBlacklistBinding

/**
 * Adapter for displaying blacklist items (video/audio files)
 */
class BlacklistAdapter(
    private val isVideo: Boolean,
    private val onDelete: (String) -> Unit
) : ListAdapter<String, BlacklistAdapter.ViewHolder>(BlacklistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlacklistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemBlacklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(fileName: String) {
            binding.tvFileName.text = fileName
            binding.ivIcon.setImageResource(
                if (isVideo) R.drawable.ic_videocam_off else R.drawable.ic_volume_off
            )
            binding.btnDelete.setOnClickListener {
                onDelete(fileName)
            }
        }
    }

    private class BlacklistDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = 
            oldItem == newItem
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = 
            oldItem == newItem
    }
}
