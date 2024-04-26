package com.willeypianotuning.toneanalyzer.ui.settings.backups

import com.dropbox.core.v2.DbxClientV2
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.di.IoDispatcher
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.sync.PianoMeterFilesImporter
import com.willeypianotuning.toneanalyzer.sync.RestoreStrategy
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RestoreBackupFileFromDropboxUseCase
@Inject
constructor(
    private val settings: AppSettings,
    @IoDispatcher private val ioDispatcher: CoroutineContext,
    private val importer: PianoMeterFilesImporter,
) {
    private fun createDropboxClient(): DbxClientV2 {
        val credentials = requireNotNull(
            DropboxClientFactory.getCredentials(settings)
        ) { "Dropbox account is not connected" }
        return DropboxClientFactory.newClient(credentials)
    }

    suspend operator fun invoke(backupFile: String, strategy: RestoreStrategy) {
        withContext(ioDispatcher) {
            val dropboxClient = createDropboxClient()
            val file = dropboxClient.files().download("/$backupFile")
            importer.importEtfz(file.inputStream, strategy)
        }
    }
}