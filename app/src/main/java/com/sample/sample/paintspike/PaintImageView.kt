package com.sample.sample.paintspike

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.util.AttributeSet
import android.graphics.PointF
import android.R.attr.y
import android.R.attr.x
import android.R.attr.spacing
import android.R.attr.mode
import android.R.attr.y
import android.R.attr.x
import android.R.attr.spacing
import android.R.attr.mode

class PaintImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr), View.OnTouchListener{

    private var downx = 0f
    private var downy = 0f
    private var upx = 0f
    private var upy = 0f

    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var paintMatrix: Matrix

    val NONE = 0
    val DRAG = 1
    val ZOOM = 2
    var mode = NONE

    var start = PointF()
    var mid = PointF()
    var oldDist = 1f
    // These matrices will be used to move and zoom image
    var savedMatrix = Matrix()
    var mMatrix = Matrix()
    var painStroke = defaultStroke

    var zoomInOrDrag = false

    companion object {
        const val defaultStroke = 15f
    }


    init {
        mMatrix.setTranslate(1f, 1f)
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener(this)
    }

    fun setNewImage(alteredBitmap: Bitmap, bmp: Bitmap) {
        canvas = Canvas(alteredBitmap)
        paint = Paint()
        paint.color = Color.parseColor("#66265c88")
        paint.strokeWidth = painStroke
        paintMatrix = Matrix()
        canvas.drawBitmap(bmp, paintMatrix, paint)

        setImageBitmap(alteredBitmap)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //For Zoom
                savedMatrix.set(paintMatrix)
                start.set(event.x, event.y)

                mode = DRAG

                //Draw
                downx = getPointerCoords(event)[0]
                downy = getPointerCoords(event)[1]
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)

                if (oldDist > 10f) {
                    savedMatrix.set(paintMatrix)
                    midPoint(mid, event)
                    mode = ZOOM

                }
            }
            MotionEvent.ACTION_UP -> {
                val xDiff = Math.abs(event.x - start.x).toInt()
                val yDiff = Math.abs(event.y - start.y).toInt()
                if (xDiff < 8 && yDiff < 8) {
                    performClick()
                }
                mode = NONE

                //Draw
                upx = getPointerCoords(event)[0]
                upy = getPointerCoords(event)[1]
                canvas.drawLine(downx, downy, upx, upy, paint)
                invalidate()

            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE

            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                zoomInOrDrag = true

                paintMatrix.set(savedMatrix)
                paintMatrix.postTranslate(event.x - start.x, event.y - start.y)
            } else if (mode == ZOOM) {
                zoomInOrDrag = true

                val newDist = spacing(event)
                if (newDist > 10f) {
                    paintMatrix.set(savedMatrix)
                    val scale = newDist / oldDist
                    painStroke = defaultStroke.div(scale)
                    paint.strokeWidth = painStroke
                    paintMatrix.postScale(scale, scale, mid.x, mid.y)
                }
            }else {
                if(!zoomInOrDrag) {
                    upx = getPointerCoords(event)[0]
                    upy = getPointerCoords(event)[1]
                    canvas.drawLine(downx, downy, upx, upy, paint)
                    invalidate()
                    downx = upx
                    downy = upy

                }
                zoomInOrDrag = false
            }
        }
        imageMatrix = paintMatrix
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        val calc = x * x + y * y
        return Math.sqrt(calc.toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun getPointerCoords(e: MotionEvent): FloatArray {
        val index = e.actionIndex
        val coords = floatArrayOf(e.getX(index), e.getY(index))
        val matrix = Matrix()
        imageMatrix.invert(matrix)
        matrix.postTranslate(scrollX.toFloat(), scrollY.toFloat())
        matrix.mapPoints(coords)
        return coords
    }

    //https://stackoverflow.com/questions/26452574/android-zooming-with-two-fingers-ontouch-and-setscalex-setscaley
}