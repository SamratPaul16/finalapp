package com.samrat.finalapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<BoundingBox> = emptyList()
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.bounding_box_color)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        style = Paint.Style.FILL
    }

    fun setResults(results: List<BoundingBox>) {
        this.results = results
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (box in results) {
            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height
            canvas.drawRect(left, top, right, bottom, paint)
            canvas.drawText(box.clsName, left, top, textPaint)
        }
    }
}
