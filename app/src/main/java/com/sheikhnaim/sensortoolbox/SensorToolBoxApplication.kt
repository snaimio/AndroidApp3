package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.app.Application              // Base class for the application
import android.content.Context              // For getting app context
import androidx.appcompat.app.AppCompatDelegate  // For setting dark/light mode

/**
 * SensorToolBoxApplication - Application class for the app
 *
 * ============================================================
 * WHAT THIS CLASS DOES:
 * ============================================================
 * 1. Runs BEFORE any activity (first code to execute when app launches)
 * 2. Sets the app theme (Light/Dark mode based on system)
 * 3. Initializes crash reporting (for debugging)
 * 4. Provides app-wide context to anywhere in the app
 * 5. Sets up global configurations
 *
 * ============================================================
 * WHY WE NEED THIS:
 * ============================================================
 * - Central place for app initialization
 * - Shared context for the whole app (getContext())
 * - Setup logging, analytics, crash reporting
 * - Configure theme before any screen appears
 *
 * ============================================================
 * HOW TO USE:
 * ============================================================
 * To get app context anywhere in the app:
 * SensorToolBoxApplication.getContext()
 *
 * Example:
 * val context = SensorToolBoxApplication.getContext()
 * Toast.makeText(context, "Hello", Toast.LENGTH_SHORT).show()
 */
class SensorToolBoxApplication : Application() {

    // ============================================================
    // COMPANION OBJECT - Like static members in Java
    // These belong to the CLASS, not to instances
    // ============================================================
    companion object {
        // Private instance - only accessible inside this class
        private lateinit var instance: SensorToolBoxApplication

        /**
         * getContext - Returns the application context
         *
         * Use this anywhere in the app to get app context
         * Example: SensorToolBoxApplication.getContext()
         *
         * @return Application context (can be used for Toast, SharedPreferences, etc.)
         */
        fun getContext(): Context {
            return instance.applicationContext
        }
    }

    /**
     * onCreate - Called when the app starts
     *
     * This is the FIRST code that runs when the app launches.
     * It runs BEFORE any activity (SplashActivity, DashboardActivity, etc.)
     *
     * WHAT HAPPENS HERE:
     * - The app instance is stored for later use
     * - Theme is configured
     * - Crash reporting is set up
     * - Logging is initialized
     */
    override fun onCreate() {
        super.onCreate()

        // Store the app instance so we can access it anywhere
        instance = this

        // ============================================================
        // STEP 1: Set the app theme (Light/Dark mode)
        // ============================================================
        setAppTheme()

        // ============================================================
        // STEP 2: Initialize crash reporting (optional)
        // ============================================================
        initializeCrashReporting()

        // ============================================================
        // STEP 3: Initialize logging
        // ============================================================
        initializeLogging()
    }

    /**
     * setAppTheme - Configures the app theme
     *
     * ============================================================
     * MODE OPTIONS:
     * ============================================================
     * 1. MODE_NIGHT_FOLLOW_SYSTEM - Follows device setting (recommended)
     *    - If device is in Dark Mode → app is Dark
     *    - If device is in Light Mode → app is Light
     *
     * 2. MODE_NIGHT_NO - Always Light mode
     *    - App is always light, regardless of device setting
     *
     * 3. MODE_NIGHT_YES - Always Dark mode
     *    - App is always dark, regardless of device setting
     *
     * ============================================================
     * WHY FOLLOW SYSTEM?
     * ============================================================
     * - Users expect apps to follow their system preference
     * - Better user experience
     * - Less eye strain at night
     */
    private fun setAppTheme() {
        // Follow system theme (Light/Dark based on device setting)
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        // ============================================================
        // ALTERNATIVE OPTIONS (uncomment to use):
        // ============================================================
        // For Light mode only:
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // For Dark mode only:
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /**
     * initializeCrashReporting - Sets up crash reporting
     *
     * WHAT THIS DOES:
     * - Captures app crashes and sends them to Firebase
     * - Helps developers find and fix bugs
     * - Only enabled for release builds (not debug)
     *
     * ============================================================
     * TO ENABLE FIREBASE CRASHLYTICS:
     * ============================================================
     * 1. Add Firebase to the project
     * 2. Uncomment the code below
     * 3. Add Firebase dependencies to build.gradle
     *
     * implementation 'com.google.firebase:firebase-crashlytics:18.6.0'
     * implementation 'com.google.firebase:firebase-analytics:21.5.0'
     */
    private fun initializeCrashReporting() {
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        // FirebaseApp.initializeApp(this)
    }

    /**
     * initializeLogging - Sets up logging for debugging
     *
     * WHAT THIS DOES:
     * - In debug mode: shows verbose logs (development)
     * - In release mode: hides verbose logs (production)
     *
     * WHY THIS MATTERS:
     * - Debug logs help find bugs during development
     * - Release logs should be minimal (security & performance)
     */
    private fun initializeLogging() {
        // Simple flag to control logging
        // Set to true for development, false for production
        val isDebug = true

        if (isDebug) {
            // Enable verbose logging for debugging
            // This will show all logs in Logcat
            android.util.Log.d("SensorToolBox", "🚀 App started in DEBUG mode")
        } else {
            // Disable verbose logging in release
            // Only show important logs
            android.util.Log.d("SensorToolBox", "🚀 App started in RELEASE mode")
        }
    }
}