package com.android.toolbag

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings

fun observerGlobal(context: Context, name: String, block: (Boolean) -> Unit) {
    observer(context, Settings.Global.getUriFor(name), block)
}

private fun observer(context: Context, uri: Uri, block: (Boolean) -> Unit) {
    context.contentResolver.registerContentObserver(uri, false, oo { block(it) })
}

fun oo(block: (Boolean) -> Unit): ContentObserver {
    return object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            block(selfChange)
        }
    }
}
