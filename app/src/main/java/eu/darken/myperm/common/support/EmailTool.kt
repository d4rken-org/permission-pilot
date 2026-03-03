package eu.darken.myperm.common.support

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import dagger.Reusable
import javax.inject.Inject

@Reusable
class EmailTool @Inject constructor() {

    data class Email(
        val recipients: List<String>,
        val subject: String,
        val body: String,
        val attachment: Uri? = null,
    )

    fun build(email: Email, offerChooser: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, email.recipients.toTypedArray())
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(Intent.EXTRA_SUBJECT, email.subject)
            putExtra(Intent.EXTRA_TEXT, email.body)

            if (email.attachment != null) {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, email.attachment)
                clipData = ClipData.newRawUri("", email.attachment)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "message/rfc822"
            }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (offerChooser) Intent.createChooser(intent, null) else intent
    }
}
