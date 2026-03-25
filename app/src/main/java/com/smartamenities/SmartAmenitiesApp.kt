package com.smartamenities

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — required by Hilt for dependency injection.
 *
 * This is the entry point Hilt uses to set up the dependency graph
 * for the entire app. Without this annotation, @Inject and @HiltViewModel
 * will crash at runtime.
 *
 * Registered in AndroidManifest.xml via android:name=".SmartAmenitiesApp"
 */
@HiltAndroidApp
class SmartAmenitiesApp : Application()
