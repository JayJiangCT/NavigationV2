package com.wonder.navigationsdkv2.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat

/**
 * Created by jiangjay on 2020/3/16
 */

fun getBitmap(ctx: Context, resourceId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(ctx, resourceId)
    val bitmap = Bitmap.createBitmap(
        drawable?.intrinsicWidth ?: 0,
        drawable?.intrinsicHeight ?: 0,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable?.setBounds(0, 0, canvas.width, canvas.height)
    drawable?.draw(canvas)
    return bitmap
}
