package com.ireddragonicy.hsrgraphicdroid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.databinding.ItemBackupBinding
import java.text.SimpleDateFormat
import java.util.*

class BackupAdapter(
    private val onRestore: (BackupData) -> Unit,
    private val onDelete: (BackupData) -> Unit
) : ListAdapter<BackupData, BackupAdapter.BackupViewHolder>(BackupDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val binding = ItemBackupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BackupViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class BackupViewHolder(private val binding: ItemBackupBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(backup: BackupData) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            
            binding.tvBackupName.text = backup.name
            binding.tvBackupDate.text = dateFormat.format(Date(backup.timestamp))
            binding.tvBackupDetails.text = "FPS: ${backup.settings.fps} | Render: ${backup.settings.renderScale}x"
            
            binding.btnRestore.setOnClickListener {
                onRestore(backup)
            }
            
            binding.btnDelete.setOnClickListener {
                onDelete(backup)
            }
        }
    }
    
    private class BackupDiffCallback : DiffUtil.ItemCallback<BackupData>() {
        override fun areItemsTheSame(oldItem: BackupData, newItem: BackupData): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
        
        override fun areContentsTheSame(oldItem: BackupData, newItem: BackupData): Boolean {
            return oldItem == newItem
        }
    }
}
