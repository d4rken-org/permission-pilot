package eu.darken.myperm.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import eu.darken.myperm.R
import eu.darken.myperm.databinding.CommonLoadingBoxViewBinding

class LoadingBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val ui = CommonLoadingBoxViewBinding.inflate(LayoutInflater.from(context), this)

    override fun onFinishInflate() {
        ui.loadingText.text = context.getString(
            when ((0..4).random()) {
                0 -> R.string.generic_loading_label_0
                1 -> R.string.generic_loading_label_1
                2 -> R.string.generic_loading_label_2
                3 -> R.string.generic_loading_label_3
                4 -> R.string.generic_loading_label_4
                5 -> R.string.generic_loading_label_5
                else -> throw IllegalArgumentException()
            }
        )
        super.onFinishInflate()
    }

}