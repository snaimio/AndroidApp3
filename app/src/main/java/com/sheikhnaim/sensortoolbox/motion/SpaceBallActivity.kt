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
import androidx.core.content.ContextCompat
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * SpaceBallActivity - A fun physics game controlled by phone tilt
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Accelerometer to detect phone tilt
 * 2. The ball moves in the direction of the tilt
 * 3. Bounces off the edges of the container
 * 4. Simple physics simulation with friction and bouncing
 *
 * ============================================================
 * PHYSICS PARAMETERS:
 * ============================================================
 * - Gravity: How much tilt affects acceleration (1.2)
 * - Friction: How much the ball slows down (0.93)
 * - Bounce Damping: Energy lost on bounce (0.65)
 * - Max Speed: Prevents ball from flying off (20)
 *
 * ============================================================
 * FIXED: Ball no longer gets stuck in the center!
 * ============================================================
 * - Added continuous physics updates
 * - Proper container dimensions
 * - Better sensitivity values
 * - Debug logging to track issues
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class SpaceBallActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Size of the ball in pixels */
        private const val BALL_SIZE_DP = 60

        /** Margin from container edges in pixels */
        private const val MARGIN_DP = 10

        /** Acceleration from tilt (higher = more responsive) */
        private const val GRAVITY = 1.2f

        /** How much the ball slows down each frame (0-1) */
        private const val FRICTION = 0.93f

        /** Energy lost on bounce (0-1) */
        private const val BOUNCE_DAMPING = 0.65f

        /** Maximum speed limit to prevent flying off */
        private const val MAX_SPEED = 20f

        /** Minimum velocity to move (prevents jitter) */
        private const val MIN_MOVEMENT = 0.1f

        /** Conversion factor: m/s to km/h */
        private const val MS_TO_KMH = 3.6f

        /** Speed threshold for "fast" status */
        private const val FAST_SPEED_THRESHOLD = 5f

        /** Tilt threshold for "idle" status */
        private const val IDLE_TILT_THRESHOLD = 0.5f

        /** Delay for fallback dimension check (ms) */
        private const val FALLBACK_DELAY_MS = 500L
    }

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
    private var ballSizePx = 0
    private var marginPx = 0

    // ============================================================
    // DEBUG FLAG - Set to true to see log messages
    // ============================================================
    private val DEBUG = false  // ✅ Changed to false by default

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_space_ball)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        getContainerDimensions()
        setupFallbackDimensionCheck()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // INITIALIZATION METHODS
    // ============================================================

    /**
     * Sets up the toolbar with title and back navigation.
     */
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.space_ball_title)
    }

    /**
     * Initializes all UI views from the layout.
     */
    private fun initializeViews() {
        ballView = findViewById(R.id.ballView)
        gameContainer = findViewById(R.id.gameContainer)
        statusText = findViewById(R.id.statusText)

        // Convert dp to pixels
        ballSizePx = dpToPixels(BALL_SIZE_DP)
        marginPx = dpToPixels(MARGIN_DP)
    }

    /**
     * Initializes the sensor manager and checks for accelerometer.
     */
    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            statusText.text = getString(R.string.space_ball_no_sensor)
            Toast.makeText(
                this,
                R.string.space_ball_sensor_not_found,
                Toast.LENGTH_LONG
            ).show()
        } else {
            statusText.text = getString(R.string.space_ball_hint)
        }
    }

    /**
     * Gets container dimensions after layout is complete.
     * This is CRITICAL - without dimensions, the ball can't move!
     */
    private fun getContainerDimensions() {
        gameContainer.post {
            containerWidth = gameContainer.width
            containerHeight = gameContainer.height

            if (DEBUG) {
                android.util.Log.d("SpaceBall", "Container: ${containerWidth}x${containerHeight}")
            }

            // Center the ball initially
            centerBall()
        }
    }

    /**
     * Fallback to get dimensions if the first attempt fails.
     */
    private fun setupFallbackDimensionCheck() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (containerWidth == 0 || containerHeight == 0) {
                containerWidth = gameContainer.width
                containerHeight = gameContainer.height
                centerBall()

                if (DEBUG) {
                    android.util.Log.d("SpaceBall", "Fallback: ${containerWidth}x${containerHeight}")
                }
            }
        }, FALLBACK_DELAY_MS)
    }

    /**
     * Centers the ball in the container.
     */
    private fun centerBall() {
        if (containerWidth > 0 && containerHeight > 0) {
            ballX = (containerWidth - ballSizePx) / 2f
            ballY = (containerHeight - ballSizePx) / 2f
            updateBallPosition()
        }
    }

    /**
     * Converts dp to pixels.
     */
    private fun dpToPixels(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

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
     *
     * @param event The sensor event containing accelerometer data
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Get tilt values
            // Negate X so the ball moves correctly based on device orientation
            val tiltX = -event.values[0]  // Left/Right tilt
            val tiltY = event.values[1]   // Forward/Backward tilt

            // Apply physics
            applyPhysics(tiltX, tiltY)

            // Check collisions with walls
            checkCollisions()

            // Update ball position on screen
            updateBallPosition()

            // Update status text
            updateStatusText(tiltX, tiltY)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for accelerometer
    }

    // ============================================================
    // PHYSICS METHODS
    // ============================================================

    /**
     * Applies physics to the ball based on tilt.
     */
    private fun applyPhysics(tiltX: Float, tiltY: Float) {
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
    }

    /**
     * Checks and handles collisions with walls.
     */
    private fun checkCollisions() {
        // Only check collisions if we have valid container dimensions
        if (containerWidth == 0 || containerHeight == 0) return

        // Left wall
        if (ballX < marginPx) {
            ballX = marginPx.toFloat()
            velocityX = -velocityX * BOUNCE_DAMPING
        }
        // Right wall
        if (ballX > containerWidth - ballSizePx - marginPx) {
            ballX = (containerWidth - ballSizePx - marginPx).toFloat()
            velocityX = -velocityX * BOUNCE_DAMPING
        }
        // Top wall
        if (ballY < marginPx) {
            ballY = marginPx.toFloat()
            velocityY = -velocityY * BOUNCE_DAMPING
        }
        // Bottom wall
        if (ballY > containerHeight - ballSizePx - marginPx) {
            ballY = (containerHeight - ballSizePx - marginPx).toFloat()
            velocityY = -velocityY * BOUNCE_DAMPING
        }
    }

    // ============================================================
    // UI UPDATE METHODS
    // ============================================================

    /**
     * Updates the ball's position on screen.
     */
    private fun updateBallPosition() {
        // Only update if we have valid dimensions
        if (containerWidth > 0 && containerHeight > 0) {
            ballView.translationX = ballX
            ballView.translationY = ballY
        }
    }

    /**
     * Updates the status text with tilt and speed information.
     */
    private fun updateStatusText(tiltX: Float, tiltY: Float) {
        val tiltMagnitude = sqrt((tiltX * tiltX + tiltY * tiltY).toDouble())
        val speed = sqrt((velocityX * velocityX + velocityY * velocityY).toDouble()).toFloat()
        val speedKmh = speed * MS_TO_KMH

        statusText.text = when {
            tiltMagnitude < IDLE_TILT_THRESHOLD && speed < FAST_SPEED_THRESHOLD ->
                getString(R.string.space_ball_hint)
            speed > FAST_SPEED_THRESHOLD ->
                getString(R.string.space_ball_fast, speedKmh.toInt())
            else ->
                getString(R.string.space_ball_status, tiltMagnitude, speedKmh)
        }
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}