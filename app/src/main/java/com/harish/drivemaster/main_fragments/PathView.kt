package com.harish.drivemaster.main_fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow

data class Marker(val levelNumber: Int, val status: LevelStatus)

enum class LevelStatus {
    COMPLETED,
    CURRENT,
    LOCKED
}

class PathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 40f
    }
    private val path = Path()
    private val markers = mutableListOf<Marker>()
    private var onMarkerClickListener: ((Marker) -> Unit)? = null

    init {
        // Initialize default styles or attributes if necessary
    }

    fun setMarkers(markerList: List<Marker>) {
        markers.clear()
        markers.addAll(markerList)
        invalidate() // Trigger a redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPath(canvas)
        drawMarkers(canvas)
    }

    private fun drawPath(canvas: Canvas) {
        // Draw the path based on markers or some predefined path
        // Example path drawing:
        paint.color = Color.LTGRAY
        paint.strokeWidth = 8f
        paint.style = Paint.Style.STROKE

        path.reset()
        path.moveTo(100f, 100f) // Start point

        // Example path drawing logic
        path.lineTo(300f, 100f)
        path.lineTo(300f, 500f)

        canvas.drawPath(path, paint)
    }

    private fun drawMarkers(canvas: Canvas) {
        markers.forEach { marker ->
            paint.color = when (marker.status) {
                LevelStatus.COMPLETED -> Color.GREEN
                LevelStatus.CURRENT -> Color.YELLOW
                LevelStatus.LOCKED -> Color.RED
            }

            // Draw marker circle
            canvas.drawCircle(100f, 100f + (marker.levelNumber * 100), 20f, paint)

            // Draw marker text
            paint.color = Color.BLACK
            paint.textSize = 40f
            canvas.drawText(marker.levelNumber.toString(), 90f, 100f + (marker.levelNumber * 100), paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // Handle marker click logic
            markers.forEach { marker ->
                val markerX = 100f
                val markerY = 100f + (marker.levelNumber * 100)

                if (Math.sqrt((x - markerX).toDouble().pow(2) + (y - markerY).toDouble().pow(2)) < 30) {
                    onMarkerClickListener?.invoke(marker)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnMarkerClickListener(listener: (Marker) -> Unit) {
        onMarkerClickListener = listener
    }
}


