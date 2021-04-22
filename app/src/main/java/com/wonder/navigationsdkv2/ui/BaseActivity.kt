package com.wonder.navigationsdkv2.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * author jiangjay on  21-04-2021
 */
abstract class BaseActivity<B : ViewBinding> : AppCompatActivity(), CoroutineScope by MainScope() {

    protected lateinit var binding: B

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding()
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    abstract fun inflateBinding() : B
}