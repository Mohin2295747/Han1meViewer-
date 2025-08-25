package com.yenaly.han1meviewer.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.model.TranslatableText

// Extension function for TextView
fun TextView.setTranslatableText(text: TranslatableText, showProgress: Boolean = true) {
    this.text = text.getDisplayText()

    if (showProgress && !text.isTranslated()) {
        // Add progress indicator
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_translation_loading)
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        compoundDrawablePadding = 8.dpToPx()
    } else {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }
}

// For Compose
@Composable
fun TranslationAwareText(
    text: TranslatableText,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    showProgress: Boolean = true,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text.getDisplayText(), style = style, modifier = Modifier.weight(1f))
        if (showProgress && !text.isTranslated()) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp).padding(start = 4.dp),
                strokeWidth = 1.5.dp,
            )
        }
    }
}

// Helper extension for dp to pixels
fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
