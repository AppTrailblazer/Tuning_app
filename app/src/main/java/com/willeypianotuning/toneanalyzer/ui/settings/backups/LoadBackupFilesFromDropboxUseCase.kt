package com.willeypianotuning.toneanalyzer.ui.settings.backups

import com.dropbox.core.v2.DbxClientV2
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.di.DefaultDispatcher
import com.willeypianotuning.toneanalyzer.di.IoDispatcher
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class LoadBackupFilesFromDropboxUseCase
@Inject constructor(
    private val settings: AppSettings,
    @IoDispatcher private val ioDispatcher: CoroutineContext,
    @DefaultDispatcher private val defaultDispatcher: CoroutineContext,
) {
    private fun createDropboxClient(): DbxClientV2 {
        val credentials = requireNotNull(
            DropboxClientFactory.getCredentials(settings)
        ) { "Dropbox account is not connected" }
        return DropboxClientFactory.newClient(credentials)
    }

    suspend operator fun invoke(): List<String> {
        val allFiles = arrayListOf<String>()
        withContext(ioDispatcher) {
            val dropboxClient = createDropboxClient()
            var result = dropboxClient.files().listFolder("")
            allFiles.addAll(result.entries.map { it.name }.filter { it.endsWith(".etfz") })
            while (result.hasMore) {
                result = dropboxClient.files().listFolderContinue(result.cursor)
                allFiles.addAll(result.entries.map { it.name }.filter { it.endsWith(".etfz") })
            }
        }
        withContext(defaultDispatcher) {
            allFiles.sortDescending()
        }
        return allFiles
    }
}