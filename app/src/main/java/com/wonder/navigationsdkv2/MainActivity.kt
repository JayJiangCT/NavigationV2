package com.wonder.navigationsdkv2

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.wonder.navigationsdkv2.databinding.ActivityMainBinding
import com.wonder.navigationsdkv2.extension.getBitmap
import com.wonder.navigationsdkv2.extension.startActivity
import com.wonder.navigationsdkv2.location.CustomerLocationProvider
import com.wonder.navigationsdkv2.ui.BaseMapActivity
import com.wonder.navigationsdkv2.utils.Utils
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.Collections

private const val RC_LOCATION = 0x70
private val perms = arrayOf(
    permission.ACCESS_FINE_LOCATION,
    permission.ACCESS_COARSE_LOCATION,
)

class MainActivity : BaseMapActivity<ActivityMainBinding>(), EasyPermissions.PermissionCallbacks {

    override val mapView: MapView
        get() = binding.mapView

    override fun inflateBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    private lateinit var locationComponent: LocationComponentPlugin

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var navigationCamera: NavigationCamera

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val locationProvider by lazy {
        CustomerLocationProvider(this)
    }

    private val centerCameraOptions by lazy {
        CameraOptions.Builder()
            .zoom(13.0)
            .pitch(0.0)
    }

    private var number: Int = 0
        set(value) {
            if (value != field) {
                binding.routeDirect.visibility = if (markers.isEmpty()) View.GONE else View.VISIBLE
                field = value
            }
        }

    private val markers = mutableListOf<Marker>()

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val pixelDensity = Resources.getSystem().displayMetrics.density

    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            100.0 * pixelDensity,
            50.0 * pixelDensity,
            100.0 * pixelDensity,
            50.0 * pixelDensity
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        permissionCheck()
        super.onCreate(savedInstanceState)
        title = "Map Page"
    }

    @SuppressLint("MissingPermission")
    override fun mapReady() {
        binding.startNavigation.setOnClickListener {
            startActivity<NavigationActivity>()
        }
        binding.locationButton.setOnClickListener {
            locationProvider.lastLocation?.let { location ->
                val point = Point.fromLngLat(location.longitude, location.latitude)
                mapboxMap.easeTo(centerCameraOptions.center(point).build())
            }
        }
        binding.cleanButton.setOnClickListener {
            routeLineApi.clearRouteLine(object :
                MapboxNavigationConsumer<Expected<RouteLineClearValue, RouteLineError>> {
                override fun accept(value: Expected<RouteLineClearValue, RouteLineError>) {
                    mapboxMap.getStyle { style ->
                        routeLineView.renderClearRouteLineValue(style, value)
                    }
                }
            })
        }
        binding.routeDirect.setOnClickListener {
            fetchRoute()
        }
        locationComponent = mapView.getLocationComponentPlugin().apply {
            locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.ic_puck
                )
            )
            setLocationProvider(locationProvider)
            enabled = true
        }
        mapView.getGesturesPlugin().addOnMapLongClickListener { point ->
            val marker = Marker(point, "layer_$number", "source_$number", "image_$number")
            markers.add(marker)
            addMarker(marker)
            number++
            false
        }
        initMapboxNavigation()
        initCamera()
        mapboxNavigation.startTripSession()
    }

    private fun addMarker(marker: Marker) {
        with(marker) {
            mapboxMap.getStyle { style ->
                if (style.getLayer(layerId) == null) {
                    style.apply {
                        addImage(imageId, getBitmap(this@MainActivity, R.drawable.mid_pointer))
                        addSource(geoJsonSource(sourceId) {
                            geometry(point)
                        })
                        addLayer(symbolLayer(layerId, sourceId) {
                            iconImage(imageId)
                        })
                    }
                }
            }
        }
    }

    private fun initMapboxNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this).accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(LocationEngineProvider.getBestLocationEngine(this)).build()
        )
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
                Log.d("TAG", "enhancedLocation: $enhancedLocation")
            }

            override fun onRawLocationChanged(rawLocation: Location) {
                Log.d("TAG", "rawLocation: $rawLocation")
                navigationCamera.requestNavigationCameraToIdle()
                val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                mapboxMap.easeTo(centerCameraOptions.center(point).build())
                mapboxNavigation.unregisterLocationObserver(this)
            }
        })
    }

    private fun initCamera() {
        viewportDataSource =
            MapboxNavigationViewportDataSource(MapboxNavigationViewportDataSourceOptions.Builder().build(), mapboxMap)
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.getCameraAnimationsPlugin(),
            viewportDataSource
        )
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun permissionCheck() {
        if (!EasyPermissions.hasPermissions(
                this@MainActivity,
                *perms
            )
        ) {
            requestPermissions()
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

    private fun fetchRoute() {
        binding.progressBar.visibility = View.VISIBLE
        locationProvider.lastLocation?.let { location ->
            val points = markers.map { it.point }.toMutableList()
            points.add(0, Point.fromLngLat(location.longitude, location.latitude))
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultParams()
                    .accessToken(Utils.getMapboxAccessToken(this))
                    .coordinates(points)
                    .steps(true)
                    .alternatives(true)
                    .continueStraight(false)
                    .voiceInstructions(true)
                    .bannerInstructions(true)
                    .voiceUnits(DirectionsCriteria.IMPERIAL)
                    .annotationsList(Collections.singletonList(DirectionsCriteria.ANNOTATION_MAXSPEED))
                    .build(),
                object : RoutesRequestCallback {
                    override fun onRoutesReady(routes: List<DirectionsRoute>) {
                        binding.progressBar.visibility = View.GONE
                        mapboxNavigation.setRoutes(routes)
                        val options = mapboxMap.cameraForGeometry(
                            LineString.fromPolyline(routes.first().geometry()!!, Constants.PRECISION_6),
                            overviewEdgeInsets,
                            0.0,
                            0.0
                        )
                        options.zoom = 16.0
                        navigationCamera.requestNavigationCameraToIdle()
                        mapboxMap.easeTo(options)
                        routeLineApi.setRoutes(routes.map { route ->
                            RouteLine(route, null)
                        }, object : MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>> {
                            override fun accept(value: Expected<RouteSetValue, RouteLineError>) {
                                mapboxMap.getStyle() { style ->
                                    routeLineView.renderRouteDrawData(style, value)
                                }
                            }
                        })
                    }

                    override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                        binding.progressBar.visibility = View.GONE
                    }
                })
        }
    }

    override fun onStop() {
        super.onStop()
        if (::navigationCamera.isInitialized) {
            navigationCamera.resetFrame()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.onDestroy()
        }
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

    data class Marker(val point: Point, val layerId: String, val sourceId: String, val imageId: String)
}