package com.wonder.navigationsdkv2.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArrayList

/**
 * author jiangjay on  21-04-2021
 */

private const val DEFAULT_INTERVAL_MILLIS = 1000L
private const val DEFAULT_FASTEST_INTERVAL_MILLIS = 1000L

class CustomerLocationProvider(context: Context) : LocationProvider, LocationEngineCallback<LocationEngineResult> {

    private val locationEngine = LocationEngineProvider.getBestLocationEngine(context)

    private val locationEngineRequest =
        LocationEngineRequest.Builder(DEFAULT_INTERVAL_MILLIS).setFastestInterval(DEFAULT_FASTEST_INTERVAL_MILLIS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY).build()

    private val locationConsumers = CopyOnWriteArrayList<LocationConsumer>()

    /**
     * Returns the last cached target location value.
     *
     * For precise puck's position use the [OnIndicatorPositionChangedListener].
     */
    var lastLocation: Location? = null
        private set

    @SuppressLint("MissingPermission")
    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        if (locationConsumers.isEmpty()) {
            requestLocationUpdates()
        }
        locationConsumers.add(locationConsumer)
        locationEngine.getLastLocation(this)
    }

    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        locationConsumers.remove(locationConsumer)
        if (locationConsumers.isEmpty()) {
            locationEngine.removeLocationUpdates(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        locationEngine.requestLocationUpdates(
            locationEngineRequest, this, Looper.getMainLooper()
        )
    }

    override fun onSuccess(result: LocationEngineResult?) {
        result?.lastLocation?.let { location ->
            lastLocation = location
            notifyLocationUpdates(location)
        }
    }

    private fun notifyLocationUpdates(location: Location) {
        locationConsumers.forEach { consumer ->
            consumer.onLocationUpdated(Point.fromLngLat(location.longitude, location.latitude))
            consumer.onBearingUpdated(location.bearing.toDouble())
        }
    }

    override fun onFailure(exception: Exception) {
        Logger.e(this::class.java.simpleName, "Failed to obtain location update: $exception")
    }
}