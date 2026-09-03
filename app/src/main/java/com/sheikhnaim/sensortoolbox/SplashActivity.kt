package com.sheikhnaim.sensortoolbox

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity - The introductory launch screen of SensorToolBox.
 *
 * How it works:
 * 1. Displays the branded splash screen layout with app icon & title.
 * 2. Uses a Handler attached to the Main Looper to delay for 2 seconds (2000ms).
 * 3. Launches DashboardActivity and calls finish() so the user cannot navigate back
 *    to the splash screen when pressing the hardware back button.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Post a delayed transition to the main dashboard on the UI thread
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, DashboardActivity::class.java))
            // Finish this activity so it is popped off the back stack
            finish()
        }, 2000)
    }
}