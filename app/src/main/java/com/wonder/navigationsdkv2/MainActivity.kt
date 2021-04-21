package com.wonder.navigationsdkv2

import android.Manifest.permission
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wonder.navigationsdkv2.databinding.ActivityMainBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

private const val RC_LOCATION = 0x70
private val perms = arrayOf(
    permission.ACCESS_FINE_LOCATION,
    permission.ACCESS_COARSE_LOCATION,
)

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var permissionEnable = false

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionCheck()
        binding.startNavigation.setOnClickListener {
            if (permissionEnable) {
                startActivity<NavigationActivity>()
            }
        }
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun permissionCheck() {
        if (!EasyPermissions.hasPermissions(
                this@MainActivity,
                *perms
            )
        ) {
            requestPermissions()
        } else {
            permissionEnable = true
        }
    }

    private fun requestPermissions() {
        EasyPermissions.requestPermissions(
            this@MainActivity,
            "We need your location, storage read and write permissions, please open it",
            RC_LOCATION,
            *perms
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}