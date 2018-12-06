package com.sample.sample.paintspike

import android.graphics.*

class Painter{
    lateinit var paint: Paint
    var isErase = false

    companion object {
        const val DEFAULT_STROKE = 25f
        private const val PAINT_COLOR = "#66265c88"
    }

    init {
        createPaint()
    }

    private fun createPaint(){
        paint = Paint()
        paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = DEFAULT_STROKE
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeCap = Paint.Cap.ROUND
        setPaintValue()
    }

    fun changeStrokeTo(strokeWidth : Float){
        paint.strokeWidth = strokeWidth
    }

    fun setEraseMode(eraseMode : Boolean){
        isErase = eraseMode
        setPaintValue()
    }

    private fun setPaintValue(){
        if(isErase){
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }else{
            paint.color = Color.parseColor(PAINT_COLOR)
            paint.xfermode = null
        }
    }

    private fun setDefaultPaint(){
        paint.color = Color.parseColor(PAINT_COLOR)
        paint.xfermode = null
    }
}