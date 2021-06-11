package com.wonder.navigationsdkv2

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.util.Stack

/**
 * author jiangjay on  07-05-2021
 */
@HiltAndroidApp
class MainApplication : Application() {

    private val activityStack by lazy {
        Stack<Int>()
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityStack.push(activity.hashCode())
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (isTopActivity(activity)) {
                    Log.d("TAG", "App has exited, ${activity::class.java.name}")
                }
                activityStack.remove(activity.hashCode())
            }
        })
    }

    fun isTopActivity(activity: Activity) = !activityStack.empty() && activityStack.first() == activity.hashCode()
}