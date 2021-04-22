package com.wonder.navigationsdkv2.ui

import android.Manifest
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.style
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)
        mapReady()
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
        mapView.onDestroy()
    }
}