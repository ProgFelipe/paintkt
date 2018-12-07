package com.sample.sample.paintspike.CustomViews

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.sample.sample.paintspike.Painter

class StrokeCircle @kotlin.jvm.JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    val paint = Paint()
    var radius = 15f
    init {
        paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.FILL
        paint.strokeWidth = Painter.DEFAULT_STROKE
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle((width/2).toFloat(), (height/2).toFloat(), radius, paint)
    }

    fun setCircleSize(value : Int){
        radius = value.toFloat()
        invalidate()
    }
}