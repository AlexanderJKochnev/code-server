package com.example.photouploader.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CrosshairView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 180
    }

    private val cells = 6

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 85% от меньшей стороны экрана — квадратная область по центру
        val side = min(width, height) * 0.85f
        val left = (width - side) / 2f
        val top = (height - side) / 2f
        val right = left + side
        val bottom = top + side
        val step = side / cells

        // Вертикальные линии
        for (i in 0..cells) {
            val x = left + i * step
            canvas.drawLine(x, top, x, bottom, paint)
        }
        // Горизонтальные линии
        for (i in 0..cells) {
            val y = top + i * step
            canvas.drawLine(left, y, right, y, paint)
        }
    }
}