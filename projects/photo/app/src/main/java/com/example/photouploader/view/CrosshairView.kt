package com.example.photouploader.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CrosshairView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 180
    }

    private val cells = 11
    private val coverage = 0.84f // 84% экрана

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 84% от ширины и высоты экрана, по центру
        val gridWidth = width * coverage
        val gridHeight = height * coverage
        val left = (width - gridWidth) / 2f
        val top = (height - gridHeight) / 2f
        val right = left + gridWidth
        val bottom = top + gridHeight

        val stepX = gridWidth / cells
        val stepY = gridHeight / cells

        // Вертикальные линии (6+1)
        for (i in 0..cells) {
            val x = left + i * stepX
            canvas.drawLine(x, top, x, bottom, paint)
        }
        // Горизонтальные линии (6+1)
        for (i in 0..cells) {
            val y = top + i * stepY
            canvas.drawLine(left, y, right, y, paint)
        }
    }
}