package com.wonder.navigationsdkv2.extension

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

/**
 * author jiangjay on  20-04-2021
 */

inline fun <reified T : AppCompatActivity> Context.startActivity(noinline block: (Intent.() -> Unit)? = null) {
    startActivity(Intent(this, T::class.java).apply {
        block?.invoke(this)
    })
}