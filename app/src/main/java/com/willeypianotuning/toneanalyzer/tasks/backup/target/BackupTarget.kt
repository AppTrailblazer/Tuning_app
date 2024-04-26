package com.willeypianotuning.toneanalyzer.tasks.backup.target

import java.io.File

interface BackupTarget {
    fun backup(backupFile: File)
}