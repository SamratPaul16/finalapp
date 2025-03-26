package com.samrat.finalapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Using the correct Handler method
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SelectionActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000)  // 5-second delay
    }
}
