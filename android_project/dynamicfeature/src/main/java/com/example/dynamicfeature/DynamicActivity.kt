package com.example.dynamicfeature

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat

class DynamicActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dynamic_activity)
        FlutterUtils.initializeFlutterEngine(applicationContext)
        findViewById<Button>(R.id.launchFlutterButton).setOnClickListener {
            startActivity(Intent(this, AppFlutterActivity::class.java))
        }
    }
}