package com.example.dynamicfeature

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.ApplicationInfoLoader
import io.flutter.embedding.engine.loader.ApplicationInfoLoader.PUBLIC_AUTOMATICALLY_REGISTER_PLUGINS_METADATA_KEY
import io.flutter.embedding.engine.loader.FlutterApplicationInfo
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.util.*

const val TAG = "FlutterUtils"

@Keep
object FlutterUtils {

    var initialized = false
    var flutterLibsPathExist = false

    private lateinit var flutterEngine: FlutterEngine

    fun initializeFlutterEngine(applicationContext: Context) {
        if (initialized) {
            Log.d(TAG, "Flutter initialize Engine is already called")
            return
        }
        Log.d(TAG, "Initializing Flutter Engine")

        var flutterLibsPath = ""

        val searchFiles: Queue<File> = LinkedList()
        // Downloaded modules are stored here
        searchFiles.add(applicationContext.filesDir)

        val aotSharedLibraryName = "libapp.so"

        while (!searchFiles.isEmpty()) {
            val file = searchFiles.remove()
            if (file != null && file.isDirectory && file.listFiles() != null) {
                for (f in file.listFiles()) {
                    searchFiles.add(f)
                }
                continue
            }
            val name = file!!.name

            if (name == aotSharedLibraryName) {
                val path = file.absolutePath.dropLast(aotSharedLibraryName.length)
                Log.d(TAG, "Flutter Library Path: $path")
                flutterLibsPath = path
                flutterLibsPathExist = true
            }
        }

        if (!flutterLibsPathExist) {
            Log.d(
                TAG, "Unable to get library paths of Flutter!. May be this is a fat apk or Flutter module is loaded " +
                        "as install time module"
            )
        }

        val injector = FlutterInjector.instance()
        val flutterLoader = injector.flutterLoader()

        flutterLoader.startInitialization(applicationContext)

        val flutterApplicationInfo = flutterLoader.javaClass.getDeclaredField("flutterApplicationInfo")
        flutterApplicationInfo.isAccessible = true

        val appInfo = getApplicationInfo(applicationContext) as ApplicationInfo
        val res = FlutterApplicationInfo(
            getString(appInfo.metaData, ApplicationInfoLoader.PUBLIC_AOT_SHARED_LIBRARY_NAME),
            getString(appInfo.metaData, ApplicationInfoLoader.PUBLIC_VM_SNAPSHOT_DATA_KEY),
            getString(appInfo.metaData, ApplicationInfoLoader.PUBLIC_ISOLATE_SNAPSHOT_DATA_KEY),
            getString(appInfo.metaData, ApplicationInfoLoader.PUBLIC_FLUTTER_ASSETS_DIR_KEY),
            getNetworkPolicy(appInfo, applicationContext),
            flutterLibsPath.ifEmpty { appInfo.nativeLibraryDir },
            getBoolean(appInfo.metaData, PUBLIC_AUTOMATICALLY_REGISTER_PLUGINS_METADATA_KEY, true)
        )

        flutterApplicationInfo.set(flutterLoader, res)
        flutterLoader.ensureInitializationComplete(applicationContext, null)

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

    private fun getApplicationInfo(applicationContext: Context): ApplicationInfo? {
        return try {
            applicationContext
                .packageManager
                .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            throw java.lang.RuntimeException(e)
        }
    }

    // Below functions are copied from package io.flutter.embedding.engine.loader.ApplicationInfoLoader

    private fun getString(metadata: Bundle?, key: String): String? {
        return metadata?.getString(key, null)
    }

    private fun getBoolean(metadata: Bundle?, key: String, defaultValue: Boolean): Boolean {
        return metadata?.getBoolean(key, defaultValue) ?: defaultValue
    }

    private fun getNetworkPolicy(appInfo: ApplicationInfo, context: Context): String? {
        // We cannot use reflection to look at networkSecurityConfigRes because
        // Android throws an error when we try to access fields marked as This member is not intended
        // for public use, and is only visible for testing..
        // Instead we rely on metadata.
        val metadata = appInfo.metaData ?: return null
        val networkSecurityConfigRes = metadata.getInt(ApplicationInfoLoader.NETWORK_POLICY_METADATA_KEY, 0)
        if (networkSecurityConfigRes <= 0) {
            return null
        }
        val output = JSONArray()
        try {
            val xrp = context.resources.getXml(networkSecurityConfigRes)
            xrp.next()
            var eventType = xrp.eventType
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {
                    if (xrp.name == "domain-config") {
                        parseDomainConfig(xrp, output, false)
                    }
                }
                eventType = xrp.next()
            }
        } catch (e: IOException) {
            return null
        } catch (e: XmlPullParserException) {
            return null
        }
        return output.toString()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseDomainConfig(
        xrp: XmlResourceParser, output: JSONArray, inheritedCleartextPermitted: Boolean
    ) {
        val cleartextTrafficPermitted = xrp.getAttributeBooleanValue(
            null, "cleartextTrafficPermitted", inheritedCleartextPermitted
        )
        while (true) {
            val eventType = xrp.next()
            if (eventType == XmlResourceParser.START_TAG) {
                if (xrp.name == "domain") {
                    // There can be multiple domains.
                    parseDomain(xrp, output, cleartextTrafficPermitted)
                } else if (xrp.name == "domain-config") {
                    parseDomainConfig(xrp, output, cleartextTrafficPermitted)
                } else {
                    skipTag(xrp)
                }
            } else if (eventType == XmlResourceParser.END_TAG) {
                break
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun skipTag(xrp: XmlResourceParser) {
        val name = xrp.name
        var eventType = xrp.eventType
        while (eventType != XmlResourceParser.END_TAG || xrp.name !== name) {
            eventType = xrp.next()
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun parseDomain(
        xrp: XmlResourceParser, output: JSONArray, cleartextPermitted: Boolean
    ) {
        val includeSubDomains = xrp.getAttributeBooleanValue(null, "includeSubdomains", false)
        xrp.next()
        check(xrp.eventType == XmlResourceParser.TEXT) { "Expected text" }
        val domain = xrp.text.trim { it <= ' ' }
        val outputArray = JSONArray()
        outputArray.put(domain)
        outputArray.put(includeSubDomains)
        outputArray.put(cleartextPermitted)
        output.put(outputArray)
        xrp.next()
        check(xrp.eventType == XmlResourceParser.END_TAG) { "Expected end of domain tag" }
    }
}

