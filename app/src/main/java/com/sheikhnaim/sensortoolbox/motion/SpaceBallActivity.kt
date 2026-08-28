package com.sheikhnaim.sensortoolbox.motion

// ============================================================
// IMPORTS
// ============================================================
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * SpaceBallActivity - A fun physics game controlled by phone tilt
 *
 * HOW IT WORKS:
 * 1. Uses the Accelerometer to detect phone tilt
 * 2. The ball moves in the direction of the tilt
 * 3. Bounces off the edges of the container
 * 4. Simple physics simulation with friction and bouncing
 *
 * FIXED: Ball no longer gets stuck in the center!
 * - Added continuous physics updates
 * - Proper container dimensions
 * - Better sensitivity values
 * - Debug logging to track issues
 */
class SpaceBallActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var ballView: View
    private lateinit var gameContainer: CardView
    private lateinit var statusText: TextView

    // ============================================================
    // BALL PHYSICS
    // ============================================================
    private var ballX = 0f
    private var ballY = 0f
    private var velocityX = 0f
    private var velocityY = 0f

    private var containerWidth = 0
    private var containerHeight = 0
    private val BALL_SIZE = 60
    private val MARGIN = 10

    // Physics constants - TUNED FOR BETTER RESPONSE
    private val GRAVITY = 1.2f          // Acceleration from tilt
    private val FRICTION = 0.93f        // How much the ball slows down
    private val BOUNCE_DAMPING = 0.65f  // Energy lost on bounce
    private val MAX_SPEED = 20f         // Maximum speed limit
    private val MIN_MOVEMENT = 0.1f     // Minimum velocity to move

    // ============================================================
    // DEBUG FLAG - Set to true to see log messages
    // ============================================================
    private val DEBUG = true

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_space_ball)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        ballView = findViewById(R.id.ballView)
        gameContainer = findViewById(R.id.gameContainer)
        statusText = findViewById(R.id.statusText)

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            statusText.text = "❌ Accelerometer not available"
            Toast.makeText(this, "Accelerometer not found", Toast.LENGTH_LONG).show()
        } else {
            statusText.text = "📱 Tilt your phone to move the ball!"
        }

        // ============================================================
        // STEP 4: Get container dimensions after layout
        // This is CRITICAL - without dimensions, the ball can't move!
        // ============================================================
        gameContainer.post {
            containerWidth = gameContainer.width
            containerHeight = gameContainer.height

            if (DEBUG) {
                android.util.Log.d("SpaceBall", "Container: ${containerWidth}x${containerHeight}")
            }

            // Center the ball initially
            ballX = (containerWidth - BALL_SIZE) / 2f
            ballY = (containerHeight - BALL_SIZE) / 2f
            updateBallPosition()
        }

        // ============================================================
        // STEP 5: Fallback - get dimensions again after a delay
        // Sometimes the first post doesn't work, so try again
        // ============================================================
        Handler(Looper.getMainLooper()).postDelayed({
            if (containerWidth == 0 || containerHeight == 0) {
                containerWidth = gameContainer.width
                containerHeight = gameContainer.height
                ballX = (containerWidth - BALL_SIZE) / 2f
                ballY = (containerHeight - BALL_SIZE) / 2f
                updateBallPosition()

                if (DEBUG) {
                    android.util.Log.d("SpaceBall", "Fallback: ${containerWidth}x${containerHeight}")
                }
            }
        }, 500)
    }

    /**
     * onResume - Register the sensor listener
     */
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /**
     * onPause - Unregister the sensor listener
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    /**
     * onSensorChanged - Called when accelerometer data changes
     *
     * HOW THE PHYSICS WORKS:
     * 1. Get tilt from accelerometer (X and Y values)
     * 2. Apply tilt as acceleration to the ball
     * 3. Apply friction to slow down
     * 4. Update position based on velocity
     * 5. Check for collisions with walls
     * 6. Update the ball's position on screen
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Get tilt values
            // Negate X so the ball moves correctly based on device orientation
            val tiltX = -event.values[0]  // Left/Right tilt
            val tiltY = event.values[1]   // Forward/Backward tilt

            // ============================================================
            // APPLY PHYSICS
            // ============================================================

            // Apply tilt as acceleration
            velocityX += tiltX * GRAVITY
            velocityY += tiltY * GRAVITY

            // Apply friction (slows down over time)
            velocityX *= FRICTION
            velocityY *= FRICTION

            // If velocity is very small, stop it completely to prevent jitter
            if (kotlin.math.abs(velocityX) < MIN_MOVEMENT) velocityX = 0f
            if (kotlin.math.abs(velocityY) < MIN_MOVEMENT) velocityY = 0f

            // Limit speed to prevent ball from flying off
            val speed = sqrt((velocityX * velocityX + velocityY * velocityY).toDouble()).toFloat()
            if (speed > MAX_SPEED) {
                velocityX = (velocityX / speed) * MAX_SPEED
                velocityY = (velocityY / speed) * MAX_SPEED
            }

            // Update position
            ballX += velocityX
            ballY += velocityY

            // ============================================================
            // CHECK COLLISIONS WITH WALLS
            // ============================================================

            // Only check collisions if we have valid container dimensions
            if (containerWidth > 0 && containerHeight > 0) {
                // Left wall
                if (ballX < MARGIN) {
                    ballX = MARGIN.toFloat()
                    velocityX = -velocityX * BOUNCE_DAMPING
                }
                // Right wall
                if (ballX > containerWidth - BALL_SIZE - MARGIN) {
                    ballX = (containerWidth - BALL_SIZE - MARGIN).toFloat()
                    velocityX = -velocityX * BOUNCE_DAMPING
                }
                // Top wall
                if (ballY < MARGIN) {
                    ballY = MARGIN.toFloat()
                    velocityY = -velocityY * BOUNCE_DAMPING
                }
                // Bottom wall
                if (ballY > containerHeight - BALL_SIZE - MARGIN) {
                    ballY = (containerHeight - BALL_SIZE - MARGIN).toFloat()
                    velocityY = -velocityY * BOUNCE_DAMPING
                }
            }

            // ============================================================
            // UPDATE BALL POSITION ON SCREEN
            // ============================================================
            updateBallPosition()

            // ============================================================
            // UPDATE STATUS TEXT WITH TILT INFO
            // ============================================================
            val tiltMagnitude = sqrt((tiltX * tiltX + tiltY * tiltY).toDouble())
            val speedKmh = speed * 3.6f // Convert to km/h for fun

            statusText.text = when {
                tiltMagnitude < 0.5 && speed < 0.5 -> "📱 Tilt your phone to move the ball!"
                speed > 5 -> "🚀 Moving fast! Speed: ${speedKmh.toInt()} km/h"
                else -> "📱 Tilt: ${"%.1f".format(tiltMagnitude)}° | Speed: ${"%.1f".format(speedKmh)} km/h"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    /**
     * updateBallPosition - Updates the ball's position on screen
     */
    private fun updateBallPosition() {
        // Only update if we have valid dimensions
        if (containerWidth > 0 && containerHeight > 0) {
            ballView.translationX = ballX
            ballView.translationY = ballY
        }
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}