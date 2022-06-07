package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest

class MainActivity : AppCompatActivity() {
    private lateinit var splitInstallManager: SplitInstallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splitInstallManager = SplitInstallManagerFactory.create(this)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.downloadFeatureButton).setOnClickListener {
            showMessage("Start module installer")
            installDynamicFeatureModule()
        }

        findViewById<Button>(R.id.launchDynamicFeatureButton).setOnClickListener {
            if (splitInstallManager.installedModules.contains("dynamicfeature")) {
                startActivity(Intent("test_dynamic_activity"))
            } else {
                showMessage("dynamic feature is not installed. Installing...")
                installDynamicFeatureModule()
            }
        }
    }

    private fun installDynamicFeatureModule() {
        val request =
            SplitInstallRequest
                .newBuilder()
                .addModule("dynamicfeature")
                .build()

        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { sessionId ->
                showMessage("installed successfully")
            }
            .addOnFailureListener { exception ->
                showMessage("installed failed")
            }
    }

    private fun showMessage(message: String) =
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show();
}