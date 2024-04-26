package com.willeypianotuning.toneanalyzer.ui.settings.backups

import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.di.IoDispatcher
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.tasks.backup.BackupManager
import com.willeypianotuning.toneanalyzer.tasks.backup.target.DropboxBackupTarget
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BackupFilesToDropboxUseCase
@Inject constructor(
    private val settings: AppSettings,
    private val backupManager: BackupManager,
    @IoDispatcher private val ioDispatcher: CoroutineContext,
) {
    suspend operator fun invoke() {
        withContext(ioDispatcher) {
            val backupDate = Date()
            val files = backupManager.getFilesForBackup(true)
            if (files.isEmpty()) {
                return@withContext
            }
            val dropboxToken =
                DropboxClientFactory.getCredentials(settings) ?: throw IllegalStateException(
                    "Dropbox account is not connected"
                )
            val backupFile = backupManager.createBackupFileFrom(files, backupDate)
            val dropBoxBackupTarget = DropboxBackupTarget(dropboxToken)
            dropBoxBackupTarget.backup(backupFile)
            settings.lastBackupDate = backupDate
        }
    }
}