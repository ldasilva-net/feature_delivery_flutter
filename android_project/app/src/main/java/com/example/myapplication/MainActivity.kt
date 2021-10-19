package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.downloadFeatureButton).setOnClickListener {
            showMessage("Start module installer")

            val splitInstallManager = SplitInstallManagerFactory.create(this)

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

        findViewById<Button>(R.id.launchDynamicFeatureButton).setOnClickListener {
            startActivity(Intent("test_dynamic_activity"))
        }
    }

    private fun showMessage(message: String) =
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show();
}