package com.ireddragonicy.hsrgraphicdroid.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.MainPagerAdapter
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.FragmentHomeBinding
import com.ireddragonicy.hsrgraphicdroid.ui.base.BaseFragment
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyTopInsetPadding()
        binding.scrollContainer.applyTopInsetPadding()
        binding.scrollContainer.applyBottomInsetPadding()

        setupButtons()
        observeStatus()

        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.refreshStatus()
            loadActiveProfile()
        }
    }

    private fun setupButtons() {
        binding.btnQuickLaunch.setOnClickListener { launchGame() }
        binding.btnQuickKill.setOnClickListener { killGame() }
        binding.btnOpenGraphics.setOnClickListener {
            // Navigate to Graphics tab using ViewPager
            (requireActivity().findViewById<ViewPager2>(R.id.viewPager))?.setCurrentItem(MainPagerAdapter.PAGE_GRAPHICS, true)
        }
        binding.btnOpenGameInfo.setOnClickListener { openGameAppInfo() }
    }

    private fun observeStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.status.collect { status ->
                    binding.chipRootStatus.text = if (status.isRootGranted) {
                        getString(R.string.root_granted)
                    } else {
                        getString(R.string.root_not_granted)
                    }
                    binding.chipRootStatus.setChipIconResource(
                        if (status.isRootGranted) R.drawable.ic_check else R.drawable.ic_close
                    )

                    binding.chipGameStatus.text = if (status.isGameInstalled) {
                        getString(R.string.game_found)
                    } else {
                        getString(R.string.game_not_found)
                    }
                    binding.chipGameStatus.setChipIconResource(
                        if (status.isGameInstalled) R.drawable.ic_check else R.drawable.ic_close
                    )

                    binding.btnQuickLaunch.isEnabled = status.isGameInstalled
                    binding.btnQuickKill.isEnabled = status.isGameInstalled && status.isRootGranted
                    binding.btnOpenGameInfo.isEnabled = status.isGameInstalled
                    binding.tvGameVersion.text = status.gameVersion?.let { "Version: $it" } ?: ""
                }
            }
        }
    }

    private suspend fun loadActiveProfile() {
        val settings = mainViewModel.readGraphicsSettings() ?: return
        val summary = getString(
            R.string.backup_details_format,
            settings.fps,
            String.format("%.1f", settings.renderScale),
            settings.getMasterQualityName(settings.graphicsQuality)
        )
        val resolution = "${settings.screenWidth}×${settings.screenHeight} • " +
                getString(if (settings.enableVSync) R.string.on else R.string.off) +
                " VSync"
        binding.tvProfileSummary.text = summary
        binding.tvResolutionSummary.text = resolution

        val gameInfo = mainViewModel.loadGameInfo()
        binding.tvGameVersion.text = gameInfo?.versionName ?: getString(R.string.not_available)
        binding.tvApkName.text = gameInfo?.apkName ?: getString(R.string.not_available)

        val storageText = gameInfo?.let {
            if (it.dataBytes != null && it.cacheBytes != null) {
                getString(
                    R.string.storage_detail_format,
                    formatBytes(it.dataBytes),
                    formatBytes(it.cacheBytes)
                )
            } else {
                getString(R.string.permission_required)
            }
        } ?: getString(R.string.permission_required)
        binding.tvStorage.text = storageText
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val unit = 1024
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    private fun launchGame() {
        val pkg = mainViewModel.currentPackage()
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = requireContext().packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openGameAppInfo() {
        val pkg = mainViewModel.currentPackage()
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        startActivity(intent)
    }

    private fun killGame() {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = mainViewModel.killGame()
            if (!success) {
                Snackbar.make(binding.root, getString(R.string.apply_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}

