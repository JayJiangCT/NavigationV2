package com.wonder.di.delegate

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KProperty

/**
 * author jiangjay on  11-06-2021
 */

inline fun <reified V : ViewBinding> AppCompatActivity.viewBinding() = DelegateBinding(V::class.java)
class DelegateBinding<V : ViewBinding>(val clazz: Class<V>) {

    var binding: V? = null

    operator fun getValue(activity: AppCompatActivity, property: KProperty<*>): V {
        return binding ?: run {
            val method = clazz.getDeclaredMethod("inflate", LayoutInflater::class.java)
            (method.invoke(null, activity.layoutInflater) as V).apply {
                binding = this
                activity.setContentView(root)
            }
        }
    }
}