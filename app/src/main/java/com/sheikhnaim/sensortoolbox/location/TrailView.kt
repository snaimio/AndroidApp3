package com.sheikhnaim.sensortoolbox.location

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * TrailView - Custom view for drawing hiking trails
 *
 * This view displays a trail path on a canvas with proper scaling
 * and centering. It converts GPS coordinates to screen coordinates
 * and draws a smooth path.
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class TrailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Width of the trail line in pixels */
        private const val STROKE_WIDTH = 8f

        /** Padding as percentage of view size (90% of view) */
        private const val PADDING_PERCENT = 0.9
    }

    // ============================================================
    // PAINT AND PATH
    // ============================================================

    /** Paint for drawing the trail path */
    private val pathPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    /** The trail path to be drawn */
    private val trailPath = Path()

    /** List of latitude/longitude points */
    private var trailPoints = mutableListOf<Pair<Double, Double>>()

    // ============================================================
    // PUBLIC METHODS
    // ============================================================

    /**
     * Updates the trail path with new points.
     *
     * @param points List of (latitude, longitude) pairs
     */
    fun updateTrail(points: List<Pair<Double, Double>>) {
        trailPoints.clear()
        trailPoints.addAll(points)
        buildPath()
        invalidate()  // Trigger redraw
    }

    /**
     * Clears the trail and resets the view.
     */
    fun clearTrail() {
        trailPoints.clear()
        trailPath.reset()
        invalidate()  // Trigger redraw
    }

    /**
     * Gets the number of points in the trail.
     */
    fun getPointCount(): Int = trailPoints.size

    /**
     * Checks if the trail has any points.
     */
    fun hasTrail(): Boolean = trailPoints.isNotEmpty()

    // ============================================================
    // PRIVATE METHODS
    // ============================================================

    /**
     * Builds the path from trail points with proper scaling.
     */
    private fun buildPath() {
        trailPath.reset()

        // Need at least 2 points to draw a line
        if (trailPoints.size < 2) return

        // Get view dimensions as Float
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // If view has no size yet, return
        if (viewWidth == 0f || viewHeight == 0f) return

        // Calculate bounds of all points (as Double)
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for ((lat, lon) in trailPoints) {
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lon < minLon) minLon = lon
            if (lon > maxLon) maxLon = lon
        }

        // Handle case where all points are the same
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        if (latRange < 0.000001 && lonRange < 0.000001) {
            // All points are the same - just draw a dot in onDraw
            return
        }

        // Calculate scale to fit points in view with padding
        val padding = (1 - PADDING_PERCENT) / 2

        // Calculate scale - convert lat/lon ranges to pixels
        // Use Double for calculations, then convert to Float for drawing
        val scaleX = (viewWidth * PADDING_PERCENT) / lonRange
        val scaleY = (viewHeight * PADDING_PERCENT) / latRange
        val scale = minOf(scaleX, scaleY).toDouble()

        // Calculate center offsets
        val midLat = (maxLat + minLat) / 2.0
        val midLon = (maxLon + minLon) / 2.0
        val offsetX = (viewWidth / 2).toDouble()
        val offsetY = (viewHeight / 2).toDouble()

        // Create path from points
        val first = trailPoints.first()
        var firstX = offsetX + ((first.second - midLon) * scale)
        var firstY = offsetY + ((first.first - midLat) * scale)

        // Clamp to view bounds (with padding)
        val paddingPxX = viewWidth * padding
        val paddingPxY = viewHeight * padding
        firstX = clamp(firstX, paddingPxX, viewWidth * (1 - padding))
        firstY = clamp(firstY, paddingPxY, viewHeight * (1 - padding))

        // Convert to Float for path (Path uses Float)
        trailPath.moveTo(firstX.toFloat(), firstY.toFloat())

        // Add all subsequent points
        for (i in 1 until trailPoints.size) {
            val point = trailPoints[i]
            var x = offsetX + ((point.second - midLon) * scale)
            var y = offsetY + ((point.first - midLat) * scale)

            // Clamp to view bounds (with padding)
            x = clamp(x, paddingPxX, viewWidth * (1 - padding))
            y = clamp(y, paddingPxY, viewHeight * (1 - padding))

            // Convert to Float for path (Path uses Float)
            trailPath.lineTo(x.toFloat(), y.toFloat())
        }
    }

    /**
     * Clamps a value between min and max.
     * Uses Double for calculations.
     */
    private fun clamp(value: Double, min: Double, max: Double): Double {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    // ============================================================
    // DRAWING METHOD
    // ============================================================

    /**
     * Draws the trail on the canvas.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear the canvas with white background
        canvas.drawColor(Color.WHITE)

        // Draw the trail path
        if (!trailPath.isEmpty) {
            canvas.drawPath(trailPath, pathPaint)
        }

        // If there's only one point, draw a dot
        if (trailPoints.size == 1) {
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val x = viewWidth / 2
            val y = viewHeight / 2

            val dotPaint = Paint().apply {
                color = Color.BLUE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(x, y, 10f, dotPaint)
        }
    }
}