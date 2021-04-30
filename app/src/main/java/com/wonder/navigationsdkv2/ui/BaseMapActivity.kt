package com.wonder.navigationsdkv2.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.viewbinding.ViewBinding
import com.mapbox.common.Logger
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.wonder.navigationsdkv2.databinding.LayoutProgressDialogBinding
import pub.devrel.easypermissions.AfterPermissionGranted

/**
 * author jiangjay on  21-04-2021
 */

private const val RC_LOCATION = 0x70
private val permissions =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

abstract class BaseMapActivity<B : ViewBinding> : BaseActivity<B>() {

    protected abstract val mapView: MapView

    open lateinit var mapboxMap: MapboxMap

    open lateinit var styleExtension: StyleContract.StyleExtension

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, {
            mapReady()
        }, object : OnMapLoadErrorListener {
            override fun onMapLoadError(mapLoadErrorType: MapLoadErrorType, message: String) {
                Logger.e(
                    "MapTag",
                    "The map initialize failed, error_type:$mapLoadErrorType, message:$message"
                )
            }
        })
    }

    @Throws(UninitializedPropertyAccessException::class)
    @AfterPermissionGranted(RC_LOCATION)
    protected abstract fun mapReady()

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
        mapView.onDestroy()
    }

    @UiThread
    open fun showProgress(prompt: String) {
        if (!dialog.isShowing) {
            if (!TextUtils.isEmpty(prompt)) {
                dialogBinding.loadingText.text = prompt
                dialogBinding.loadingText.visibility = View.VISIBLE
            }
            dialog.show()
        }
    }

    @UiThread
    open fun hideProgress() {
        if (dialog.isShowing) {
            dialogBinding.loadingText.visibility = View.GONE
            dialog.hide()
        }
    }
}