package com.example.dynamicfeature

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import dalvik.system.PathClassLoader
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor

const val TAG = "FlutterUtils"

@Keep
object FlutterUtils {

    var initialized = false

    private lateinit var flutterEngine: FlutterEngine

    fun initializeFlutterEngine(applicationContext: Context) {
        if (initialized) {
            Log.d(TAG, "Flutter initialize Engine is already called")
            return
        }
        Log.d(TAG, "Initializing Flutter Engine")

        val injector = FlutterInjector.instance()
        val flutterLoader = injector.flutterLoader()
        flutterLoader.startInitialization(applicationContext)
        val pathLoader = applicationContext.classLoader as PathClassLoader
        val path = pathLoader.findLibrary("app")
        val shell = "--aot-shared-library-name=$path"
        flutterLoader.ensureInitializationComplete(applicationContext, arrayOf(shell))

        val jni = injector.flutterJNIFactory.provideFlutterJNI()
        flutterEngine = FlutterEngine(applicationContext, flutterLoader, jni)

        // Start executing Dart code to pre-warm the FlutterEngine.
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        // Cache the FlutterEngine to be used by FlutterActivity.
        FlutterEngineCache
            .getInstance()
            .put(AppConstants.FLUTTER_MAIN_ENGINE_ID, flutterEngine)

        initialized = true
    }
}

