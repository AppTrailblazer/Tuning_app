package com.willeypianotuning.toneanalyzer.tasks.backup.target

import com.dropbox.core.v2.files.WriteMode
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxCredentials
import java.io.File
import java.io.FileInputStream

class DropboxBackupTarget(private val credentials: DropboxCredentials) : BackupTarget {

    override fun backup(backupFile: File) {
        val dropboxClient = DropboxClientFactory.newClient(credentials)

        FileInputStream(backupFile).use {
            dropboxClient.files()
                .uploadBuilder("/" + backupFile.name)
                .withMode(WriteMode.OVERWRITE)
                .withMute(true)
                .uploadAndFinish(it)
        }
    }

}