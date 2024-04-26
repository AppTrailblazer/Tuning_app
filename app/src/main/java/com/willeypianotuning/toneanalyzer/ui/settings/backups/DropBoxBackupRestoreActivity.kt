package com.willeypianotuning.toneanalyzer.ui.settings.backups

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dropbox.core.android.Auth
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.ActivityRestoreFromDropboxBinding
import com.willeypianotuning.toneanalyzer.extensions.runWithNotificationsPermission
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.sync.RestoreStrategy
import com.willeypianotuning.toneanalyzer.tasks.backup.BackupWorker
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.utils.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class DropBoxBackupRestoreActivity : BaseActivity() {
    private val viewModel: DropBoxBackupRestoreViewModel by viewModels()

    private lateinit var binding: ActivityRestoreFromDropboxBinding

    private val adapter: BackupFileListAdapter by lazy { BackupFileListAdapter() }

    private var progressDialog: ProgressDialog? = null

    private val backupIntervals: List<Pair<String, Long>> by lazy {
        listOf(
            getString(R.string.dropbox_action_disconnect) to ACTION_DISCONNECT,
            getString(R.string.backups_frequency_never) to 0L,
            getString(R.string.backups_frequency_daily) to TimeUnit.DAYS.toMillis(1),
            getString(R.string.backups_frequency_weekly) to TimeUnit.DAYS.toMillis(7),
            getString(R.string.backups_frequency_monthly) to TimeUnit.DAYS.toMillis(30),
            getString(R.string.backups_frequency_yearly) to TimeUnit.DAYS.toMillis(365)
        )
    }

    private val settingsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> updateUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestoreFromDropboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.global_settings_backup_dropbox)

        adapter.itemClickListener = { item ->
            selectRestoreStrategyForBackup(item)
        }
    }

    private fun selectRestoreStrategyForBackup(item: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dropbox_restore_dialog_title))
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.dropbox_restore_strategy_missing),
                    getString(R.string.dropbox_restore_strategy_overwrite_existing),
                    getString(R.string.dropbox_restore_strategy_overwrite_all)
                ), -1
            ) { dialogInterface, i ->
                dialogInterface.dismiss()
                viewModel.restoreFromBackup(item, RestoreStrategy.values()[i])
            }
            .setNegativeButton(R.string.action_cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
        dialog.show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        binding.backupFilesRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.backupFilesRecyclerView.adapter = adapter
        binding.backupFilesRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                RecyclerView.VERTICAL
            )
        )

        binding.retryButton.setDebounceOnClickListener {
            viewModel.loadFiles()
        }

        viewModel.screenState.observe(this) {
            onScreenStateChanged(it)
        }

        viewModel.backupState.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null
            when (it) {
                is DropBoxBackupRestoreViewModel.BackupToDropBoxState.Error -> {
                    Toast.makeText(
                        this,
                        getString(R.string.dropbox_message_backup_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetRestoreState()
                }
                is DropBoxBackupRestoreViewModel.BackupToDropBoxState.Loading -> {
                    progressDialog = ProgressDialog.show(
                        this,
                        getString(R.string.app_name),
                        getString(R.string.dropbox_message_performing_backup),
                        true,
                        false
                    )
                    progressDialog?.show()
                }
                is DropBoxBackupRestoreViewModel.BackupToDropBoxState.Success -> {
                    Toast.makeText(
                        this,
                        getString(R.string.dropbox_message_backup_successfull),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetBackupState()
                    updateUi()
                }
                else -> {}
            }
        }

        viewModel.restoreState.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null
            when (it) {
                is DropBoxBackupRestoreViewModel.RestoreFromDropBoxState.Error -> {
                    Toast.makeText(
                        this,
                        getString(R.string.dropbox_message_restore_from_backup_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetRestoreState()
                }
                is DropBoxBackupRestoreViewModel.RestoreFromDropBoxState.Loading -> {
                    progressDialog = ProgressDialog.show(
                        this,
                        getString(R.string.app_name),
                        getString(R.string.dropbox_message_restoring_backup),
                        true,
                        false
                    )
                    progressDialog?.show()
                }
                is DropBoxBackupRestoreViewModel.RestoreFromDropBoxState.Success -> {
                    Toast.makeText(
                        this,
                        getString(R.string.dropbox_message_backup_restored_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetRestoreState()
                }
                else -> {}
            }
        }

        binding.dropboxConnectButton.setDebounceOnClickListener {
            runWithNotificationsPermission {
                if (DropboxClientFactory.getCredentials(appSettings) != null) {
                    onBackupFrequencyClicked()
                } else {
                    Auth.startOAuth2PKCE(
                        this,
                        getString(R.string.dropbox_app_key),
                        DropboxClientFactory.newRequestConfig(),
                        DropboxClientFactory.getScope()
                    )
                }
            }
        }

        binding.listItemDropboxBackupNow.setDebounceOnClickListener { onBackupNowClicked() }
    }

    override fun onStart() {
        super.onStart()
        updateUi()
        appSettings.prefs.registerOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    override fun onStop() {
        appSettings.prefs.unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
        super.onStop()
    }

    private fun updateUi() {
        val dropboxAuthenticated = DropboxClientFactory.getCredentials(appSettings) != null
        binding.dropboxStateTextView.setText(if (dropboxAuthenticated) R.string.dropbox_message_connected else R.string.dropbox_message_not_connected)

        if (!dropboxAuthenticated) {
            binding.backupFilesLabelTextView.visibility = View.GONE
            binding.listItemDropboxBackupNow.visibility = View.GONE
            onScreenStateChanged(DropBoxBackupRestoreViewModel.DropBoxScreenState.Unauthenticated)
        } else {
            binding.backupFilesLabelTextView.visibility = View.VISIBLE
            binding.listItemDropboxBackupNow.visibility = View.VISIBLE

            if (viewModel.screenState.value is DropBoxBackupRestoreViewModel.DropBoxScreenState.Unauthenticated
                || viewModel.screenState.value is DropBoxBackupRestoreViewModel.DropBoxScreenState.Error
            ) {
                viewModel.loadFiles()
            }
        }

        val lastBackupDate = appSettings.lastBackupDate
        if (lastBackupDate == null) {
            binding.textViewSettingBackupLastDate.text = getString(
                R.string.dropbox_message_last_backup,
                getString(R.string.dropbox_message_last_backup_never)
            )
        } else {
            val dateFormat = DateFormat.getDateTimeInstance()
            binding.textViewSettingBackupLastDate.text =
                getString(R.string.dropbox_message_last_backup, dateFormat.format(lastBackupDate))
        }

        if (dropboxAuthenticated) {
            val currentBackupInterval =
                backupIntervals.firstOrNull { it.second == appSettings.backupRepeatInterval() }
                    ?: backupIntervals.first { it.first == getString(R.string.backups_frequency_weekly) }
            binding.dropboxConnectButton.text = currentBackupInterval.first
        } else {
            binding.dropboxConnectButton.setText(R.string.dropbox_action_connect)
        }
    }

    private fun onBackupFrequencyClicked() {
        val repeatInterval = appSettings.backupRepeatInterval()
        var selectedIndex = 1
        for (i in backupIntervals.indices) {
            if (backupIntervals[i].second == repeatInterval) {
                selectedIndex = i
                break
            }
        }
        val backupIntervalNames = arrayOfNulls<String>(backupIntervals.size)
        for (i in backupIntervals.indices) {
            backupIntervalNames[i] = backupIntervals[i].first
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.global_settings_backup_dropbox))
            .setSingleChoiceItems(backupIntervalNames, selectedIndex) { dialogInterface, i ->
                dialogInterface.dismiss()
                if (backupIntervals[i].second == ACTION_DISCONNECT) {
                    DropboxClientFactory.removeCredentials(appSettings)
                    updateUi()
                    viewModel.loadFiles()
                } else {
                    appSettings.setBackupRepeatInterval(backupIntervals[i].second)
                    BackupWorker.scheduleAutomaticBackup(this, true)
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialogInterface, _ -> dialogInterface.cancel() }
            .create()
        dialog.show()
    }

    private fun onBackupNowClicked() {
        if (!NetworkUtil.isNetworkConnected(applicationContext)) {
            Toast.makeText(
                this,
                getString(R.string.dropbox_error_message_no_internet_connection),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        runWithNotificationsPermission {
            viewModel.backupNow()
        }
    }

    private fun onScreenStateChanged(state: DropBoxBackupRestoreViewModel.DropBoxScreenState) {
        when (state) {
            is DropBoxBackupRestoreViewModel.DropBoxScreenState.Unauthenticated -> {
                binding.dataLayout.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
                binding.loadingLayout.visibility = View.GONE
            }
            is DropBoxBackupRestoreViewModel.DropBoxScreenState.Loading -> {
                binding.dataLayout.visibility = View.GONE
                binding.errorLayout.visibility = View.GONE
                binding.loadingLayout.visibility = View.VISIBLE
            }
            is DropBoxBackupRestoreViewModel.DropBoxScreenState.Error -> {
                binding.dataLayout.visibility = View.GONE
                binding.errorLayout.visibility = View.VISIBLE
                binding.loadingLayout.visibility = View.GONE
            }
            is DropBoxBackupRestoreViewModel.DropBoxScreenState.Data -> {
                adapter.replaceAll(state.backupFiles)
                binding.dataLayout.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
                binding.loadingLayout.visibility = View.GONE
            }
        }
    }

    companion object {
        const val ACTION_DISCONNECT = -1L
    }
}
