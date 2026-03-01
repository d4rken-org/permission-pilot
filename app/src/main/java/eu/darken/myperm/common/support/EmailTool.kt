package eu.darken.myperm.common.support

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Reusable
class EmailTool @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun build(
        recipient: String,
        subject: String,
        body: String,
        attachmentUri: Uri? = null,
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)

            if (attachmentUri != null) {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                clipData = ClipData.newRawUri("", attachmentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "message/rfc822"
            }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return Intent.createChooser(intent, null)
    }
}
