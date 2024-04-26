package com.willeypianotuning.toneanalyzer.store.dropbox

import com.dropbox.core.oauth.DbxCredential

sealed class DropboxCredentials {
    @Deprecated("Use short-lived token instead")
    class LongLivedToken(val accessToken: String) : DropboxCredentials()
    class ShortLivedToken(val credentials: DbxCredential) : DropboxCredentials()
}
