package com.wonder.navigationsdkv2.ui

import android.util.Log
import android.view.View
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions.Builder
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.api.ManeuverCallback
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.maneuver.api.UpcomingManeuverListCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepDistanceError
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.wonder.navigationsdkv2.databinding.ActivityNavigationBinding

/**
 * author jiangjay on  25-04-2021
 */

class NavigationActivity : BaseNavigationActivity<ActivityNavigationBinding>(), UpcomingManeuverListCallback,
    StepDistanceRemainingCallback, ManeuverCallback {

    private val tripProgressApi by lazy {
        MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(
                        mapboxNavigation.navigationOptions.distanceFormatterOptions
                    )
                )
                .timeRemainingFormatter(TimeRemainingFormatter(this))
                .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(
                        this,
                        TimeFormat.NONE_SPECIFIED
                    )
                ).build()
        )
    }

    private val maneuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(Builder(this).build()))
    }

    override val mapView: MapView
        get() = binding.mapView

    override fun inflateBinding(): ActivityNavigationBinding = ActivityNavigationBinding.inflate(layoutInflater)

    override fun mapReady() {
        binding.recenter.setOnClickListener {
            updateCameraToFollowing()
            binding.recenter.visibility = View.GONE
        }
        binding.stop.setOnClickListener {
            stopNavigation()
            binding.recenter.visibility = View.GONE
            binding.maneuverView.visibility = View.GONE
            binding.tripProgressCard.visibility = View.GONE
        }
        binding.tripProgressCard.visibility = View.VISIBLE
    }

    override fun routeProgressChanged(routeProgress: RouteProgress) {
        binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress))
        maneuverApi.getUpcomingManeuverList(routeProgress, this)
        ifNonNull(routeProgress.currentLegProgress) { legProgress ->
            ifNonNull(legProgress.currentStepProgress) {
                maneuverApi.getStepDistanceRemaining(it, this)
            }
        }
    }

    override fun onUpcomingManeuvers(maneuvers: Expected<List<Maneuver>, ManeuverError>) {
        when (maneuvers) {
            is Expected.Success -> {
                binding.maneuverView.renderUpcomingManeuvers(maneuvers.value)
            }
            is Expected.Failure -> {

            }
            else -> Unit
        }
    }

    override fun onStepDistanceRemaining(distanceRemaining: Expected<StepDistance, StepDistanceError>) {
        when (distanceRemaining) {
            is Expected.Success -> {
                binding.maneuverView.renderDistanceRemaining(distanceRemaining.value)
            }
            is Expected.Failure -> {

            }
            else -> Unit
        }
    }

    override fun maneuverInstructionChanged(bannerInstructions: BannerInstructions) {
        if (binding.maneuverView.visibility != View.VISIBLE) {
            binding.maneuverView.visibility = View.VISIBLE
        }
        maneuverApi.getManeuver(bannerInstructions, this)
    }

    override fun onManeuver(maneuver: Expected<Maneuver, ManeuverError>) {
        binding.maneuverView.renderManeuver(maneuver)
    }
}