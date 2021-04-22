package com.wonder.navigationsdkv2

import android.content.Intent
import android.os.Bundle
import com.wonder.navigationsdkv2.databinding.ActivityTestBinding
import com.wonder.navigationsdkv2.extension.startActivity
import com.wonder.navigationsdkv2.ui.BaseActivity

/**
 * author jiangjay on  21-04-2021
 */
class TestActivity : BaseActivity<ActivityTestBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.start.setOnClickListener {
            startActivity(Intent(this, NavigationActivity::class.java))
        }
    }

    override fun inflateBinding(): ActivityTestBinding = ActivityTestBinding.inflate(layoutInflater)
}