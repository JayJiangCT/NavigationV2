package com.wonder.navigationsdkv2.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.inferDeviceLanguage
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionState.STARTED
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingArrivalApi
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingHighlightApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.wonder.navigationsdkv2.R
import com.wonder.navigationsdkv2.extension.startActivity
import com.wonder.navigationsdkv2.utils.Utils

/**
 * author jiangjay on  20-04-2021
 */

inline fun <reified T : BaseNavigationActivity<out ViewBinding>> Context.startNavigationActivity(
    route: DirectionsRoute,
    simulated: Boolean
) {
    startActivity<T> {
        putExtra("extra_route", route)
        putExtra("is_simulated", simulated)
    }
}

private const val TAG = "BaseNavigationActivity"

abstract class BaseNavigationActivity<B : ViewBinding> : BaseMapActivity<B>() {

    protected val route: DirectionsRoute
        get() = intent.getSerializableExtra("extra_route") as DirectionsRoute

    private val simulated: Boolean
        get() = intent.getBooleanExtra("is_simulated", false)

    protected lateinit var mapboxNavigation: MapboxNavigation

    private var isNavigating = false

    /**
     * Location
     */

    private lateinit var locationComponent: LocationComponentPlugin

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    /**
     * Camera
     */
    protected lateinit var navigationCamera: NavigationCamera

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val centerCameraOptions by lazy {
        CameraOptions.Builder()
            .zoom(17.0)
            .pitch(45.0)
            .padding(followingEdgeInsets)
    }

    /**
     * EdgeInsets
     */
    private val pixelDensity = Resources.getSystem().displayMetrics.density

    private val followingEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            mapboxMap.getSize().height.toDouble() * 2.0 / 3.0,
            0.0 * pixelDensity,
            0.0 * pixelDensity,
            0.0 * pixelDensity
        )
    }

    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            40.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private val landscapeFollowingEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            mapboxMap.getSize().height.toDouble() * 2.0 / 5.0,
            mapboxMap.getSize().width.toDouble() / 2.0,
            0.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    /**
     * Route Render
     */
    private var routeLineApi: MapboxRouteLineApi? = null

    private var routeLineView: MapboxRouteLineView? = null

    private var routeArrowView: MapboxRouteArrowView? = null

    private var routeArrowApi: MapboxRouteArrowApi? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        routeLineApi?.updateTraveledRouteLine(point)
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                val selectedRoute = routes.first()
                routeLineApi?.setRoutes(listOf(RouteLine(selectedRoute, null)), object :
                    MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>> {
                    override fun accept(value: Expected<RouteSetValue, RouteLineError>) {
                        ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                            view.renderRouteDrawData(style, value)
                            updateCameraToFollowing()
                        }
                    }
                })
                if (simulated) {
                    startSimulation(selectedRoute)
                }
                viewportDataSource.onRouteChanged(selectedRoute)
            } else {
                viewportDataSource.clearRouteData()
                navigationCamera.requestNavigationCameraToIdle()
                clearRouteLine()
            }
        }
    }

    /**
     * Simulated engine
     */

    protected val mapboxReplayer by lazy {
        MapboxReplayer()
    }

    private val replayProgressObserver by lazy {
        ReplayProgressObserver(mapboxReplayer)
    }

    private val replayRouteMapper by lazy {
        ReplayRouteMapper()
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()
            ifNonNull(routeArrowApi, routeArrowView, mapboxMap.getStyle()) { api, view, style ->
                api.addUpcomingManeuverArrow(routeProgress).apply {
                    view.renderManeuverUpdate(style, this)
                }
            }
            routeProgressChanged(routeProgress)
        }
    }

    /**
     * Speech
     */

    private var speechApi: MapboxSpeechApi? = null

    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null

    private val speechCallback =
        object : MapboxNavigationConsumer<Expected<SpeechValue, SpeechError>> {
            override fun accept(value: Expected<SpeechValue, SpeechError>) {
                when (value) {
                    is Expected.Success -> {
                        val currentSpeechValue = value.value
                        voiceInstructionsPlayer?.play(
                            currentSpeechValue.announcement,
                            voiceInstructionsPlayerCallback
                        )
                    }
                    is Expected.Failure -> {
                        val currentSpeechError = value.error
                        voiceInstructionsPlayer?.play(
                            currentSpeechError.fallback,
                            voiceInstructionsPlayerCallback
                        )
                    }
                }
            }
        }

    private val voiceInstructionsPlayerCallback =
        object : MapboxNavigationConsumer<SpeechAnnouncement> {
            override fun accept(value: SpeechAnnouncement) {
                speechApi?.clean(value)
            }
        }

    private val voiceInstructionsObserver = object : VoiceInstructionsObserver {
        override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
            speechApi?.generate(
                voiceInstructions,
                speechCallback
            )
        }
    }

    /**
     * Banner Instruction
     */
    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            maneuverInstructionChanged(bannerInstructions)
        }
    }

    /**
     * Building Arrival
     */
    private var buildingsArrivalApi: MapboxBuildingArrivalApi? = null

    private val mapMatcherResultObserver = object : MapMatcherResultObserver {
        override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
            val transitionOptions: (ValueAnimator.() -> Unit)? = if (mapMatcherResult.isTeleport) {
                {
                    duration = 0
                }
            } else {
                {
                    duration = 1000
                }
            }
            navigationLocationProvider.changePosition(
                mapMatcherResult.enhancedLocation,
                mapMatcherResult.keyPoints,
                latLngTransitionOptions = transitionOptions,
                bearingTransitionOptions = transitionOptions
            )
            viewportDataSource.onLocationChanged(mapMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
            if (mapMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
        }
    }

    /**
     * Trip
     */
    private val tripStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            if (tripSessionState == STARTED) {
                //Navigation starts
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLocationComponent()
        initMapboxNavigation()
        initCamera()
        initRouteLine()
        initSpeech()
        startNavigation()
    }

    private fun initLocationComponent() {
        locationComponent = mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@BaseNavigationActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            enabled = true
        }
    }

    private fun initMapboxNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(
                    if (simulated) ReplayLocationEngine(mapboxReplayer)
                    else LocationEngineProvider.getBestLocationEngine(this)
                )
                .build()
        ).apply {
            registerLocationObserver(object : LocationObserver {
                override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
                    Log.d("TAG", "enhancedLocation: $enhancedLocation")
                }

                override fun onRawLocationChanged(rawLocation: Location) {
                    Log.d("TAG", "rawLocation: $rawLocation")
                    navigationCamera.requestNavigationCameraToIdle()
                    val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                    mapboxMap.easeTo(centerCameraOptions.center(point).build())
                    navigationLocationProvider.changePosition(rawLocation)
                    mapboxNavigation.unregisterLocationObserver(this)
                }
            })
            registerTripSessionStateObserver(tripStateObserver)
            registerRoutesObserver(routesObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerMapMatcherResultObserver(mapMatcherResultObserver)
            registerVoiceInstructionsObserver(voiceInstructionsObserver)
            registerBannerInstructionsObserver(bannerInstructionsObserver)
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun initCamera() {
        val debugger = MapboxNavigationViewportDataSourceDebugger(this, mapView)
        debugger.enabled = true
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        viewportDataSource.debugger = debugger
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
        navigationCamera.debugger = debugger
        mapView.camera.addCameraAnimationsLifecycleListener(NavigationBasicGesturesHandler(navigationCamera))
    }

    private fun initRouteLine() {
        val mapboxRouteLineOptions =
            MapboxRouteLineOptions.Builder(this).withRouteLineBelowLayerId("road-label").build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        routeArrowApi = MapboxRouteArrowApi()
    }

    private fun initSpeech() {
        speechApi = MapboxSpeechApi(
            this,
            Utils.getMapboxAccessToken(this),
            inferDeviceLanguage()
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            Utils.getMapboxAccessToken(this),
            inferDeviceLanguage()
        )
    }

    private fun initBuildingArrival() {
        buildingsArrivalApi?.buildingHighlightApi(MapboxBuildingHighlightApi(mapboxMap))
        buildingsArrivalApi?.enable(mapboxNavigation)
    }

    @SuppressLint("MissingPermission")
    private fun startNavigation() {
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.startTripSession()
            mapboxNavigation.setRoutes(listOf(route))
        }
    }

    private fun clearRouteLine() {
        ifNonNull(routeLineApi, routeLineView, mapboxMap.getStyle()) { api, view, style ->
            api.clearRouteLine(
                object : MapboxNavigationConsumer<Expected<RouteLineClearValue, RouteLineError>> {
                    override fun accept(value: Expected<RouteLineClearValue, RouteLineError>) {
                        view.renderClearRouteLineValue(style, value)
                    }
                }
            )
        }
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    override fun onStop() {
        super.onStop()
        navigationCamera.resetFrame()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapboxNavigation.isInitialized) {
            if (simulated) {
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            }
            mapboxNavigation.unregisterTripSessionStateObserver(tripStateObserver)
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterMapMatcherResultObserver(mapMatcherResultObserver)
            mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
            mapboxNavigation.onDestroy()
        }
        speechApi?.cancel()
        voiceInstructionsPlayer?.shutdown()
        buildingsArrivalApi?.disable()
    }

    protected fun updateCameraToFollowing() {
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingEdgeInsets
        } else {
            viewportDataSource.followingPadding = overviewEdgeInsets
        }
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()
    }

    protected fun stopNavigation() {
        navigationCamera.requestNavigationCameraToIdle()
        mapboxNavigation.setRoutes(emptyList())
        clearRouteLine()
    }

    protected abstract fun routeProgressChanged(routeProgress: RouteProgress)

    protected abstract fun maneuverInstructionChanged(bannerInstructions: BannerInstructions)
}