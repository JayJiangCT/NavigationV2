package com.wonder.navigationsdkv2.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.wonder.navigationsdkv2.R

/**
 * author jiangjay on  20-04-2021
 */
abstract class BaseNavigation<B : ViewBinding> : BaseActivity<B>() {

    protected lateinit var mapboxMap: MapboxMap

    protected lateinit var mapboxNavigation: MapboxNavigation

    protected lateinit var navigationCamera: NavigationCamera

    private lateinit var locationComponent: LocationComponentPlugin

    protected abstract val mapView: MapView

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val onIndicatorPositionChangedListener by lazy {
        OnIndicatorPositionChangedListener { point ->
            // TODO: 4/21/2021 update route line
        }
    }

    private val onIndicatorBearingChangedListener by lazy {
        OnIndicatorBearingChangedListener { bearing ->
            // TODO: 4/21/2021 update route line
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapboxMap = mapView.getMapboxMap()
        initLocationComponent()
    }

    private fun initLocationComponent() {
        locationComponent = mapView.getLocationComponentPlugin().apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@BaseNavigation,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            enabled = true
        }
    }
}