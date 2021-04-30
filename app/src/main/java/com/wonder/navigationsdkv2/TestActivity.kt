package com.wonder.navigationsdkv2

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import com.wonder.navigationsdkv2.databinding.ActivityTestBinding
import com.wonder.navigationsdkv2.databinding.LayoutProgressDialogBinding
import com.wonder.navigationsdkv2.ui.BaseActivity

/**
 * author jiangjay on  21-04-2021
 */
class TestActivity : BaseActivity<ActivityTestBinding>() {

    private var _dialogBinding: LayoutProgressDialogBinding? = null

    private val dialogBinding: LayoutProgressDialogBinding
        get() = _dialogBinding!!

    private val dialog by lazy {
        _dialogBinding = LayoutProgressDialogBinding.inflate(layoutInflater)
        Dialog(this).apply {
            setCancelable(true)
            setContentView(dialogBinding.root)
        }
    }

    override fun onBackPressed() {
        if (dialog.isShowing) {
            dialog.hide()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.start.setOnClickListener {
            if (!dialog.isShowing) {
                dialog.show()
            }
        }
    }

    override fun inflateBinding(): ActivityTestBinding = ActivityTestBinding.inflate(layoutInflater)
}