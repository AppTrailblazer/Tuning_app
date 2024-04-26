package com.willeypianotuning.toneanalyzer.store.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.android.AuthActivity
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.willeypianotuning.toneanalyzer.AppSettings

object DropboxClientFactory {
    fun newRequestConfig(): DbxRequestConfig {
        return DbxRequestConfig.newBuilder("pianometer-android-app")
            .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
            .build()
    }

    @Suppress("DEPRECATION")
    fun getCredentials(settings: AppSettings): DropboxCredentials? {
        val oldAccessToken = settings.dropboxAccessToken
        if (oldAccessToken != null) {
            return DropboxCredentials.LongLivedToken(oldAccessToken)
        }
        var newCredentials = settings.dropboxCredentials
        if (newCredentials == null) {
            newCredentials = Auth.getDbxCredential()?.also {
                settings.dropboxCredentials = it
            }
        }
        if (newCredentials != null) {
            return DropboxCredentials.ShortLivedToken(newCredentials)
        }
        return null
    }

    fun removeCredentials(settings: AppSettings) {
        AuthActivity.result = null
        settings.dropboxCredentials = null
    }

    fun getScope(): List<String> {
        return listOf(
            // always required
            "account_info.read",
            // list files inside PianoMeter folder
            "files.metadata.read",
            // we need to have READ/WRITE access to files in PianoMeter folder
            "files.content.read",
            "files.content.write"
        )
    }

    fun newClient(credentials: DropboxCredentials): DbxClientV2 {
        return when (credentials) {
            is DropboxCredentials.ShortLivedToken -> DbxClientV2(
                newRequestConfig(),
                credentials.credentials
            )
            is DropboxCredentials.LongLivedToken -> DbxClientV2(
                newRequestConfig(),
                credentials.accessToken
            )
        }
    }
}