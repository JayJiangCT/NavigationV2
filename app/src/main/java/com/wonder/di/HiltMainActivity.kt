package com.wonder.di

import com.wonder.navigationsdkv2.databinding.ActivityHiltMainBinding
import com.wonder.navigationsdkv2.ui.BaseActivity

/**
 * author jiangjay on  11-06-2021
 */
class HiltMainActivity : BaseActivity<ActivityHiltMainBinding>() {

    override fun inflateBinding(): ActivityHiltMainBinding = ActivityHiltMainBinding.inflate(layoutInflater)
}