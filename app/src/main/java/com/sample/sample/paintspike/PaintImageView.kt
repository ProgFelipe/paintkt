package com.sample.sample.paintspike

import android.content.Context
import android.graphics.*
import android.widget.ImageView
import android.util.AttributeSet
import android.graphics.PointF
import android.support.v4.view.GestureDetectorCompat
import android.view.*

//https://stackoverflow.com/questions/19418878/implementing-pinch-zoom-and-drag-using-androids-build-in-gesture-listener-and-s
//https://stackoverflow.com/questions/26452574/android-zooming-with-two-fingers-ontouch-and-setscalex-setscaley

class PaintImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr),
    View.OnTouchListener,
    GestureDetector.OnGestureListener, IPaintView{

    private var mGestureDetector : GestureDetectorCompat
    private var mScaleGestureDetector: ScaleGestureDetector? = null

    private var downx = 0f
    private var downy = 0f
    private var upx = 0f
    private var upy = 0f

    var lastFocusX: Float = 0f
    var lastFocusY: Float = 0f

    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private lateinit var paintMatrix: Matrix

    var mode = NONE

    var start = PointF()
    var mid = PointF()
    var oldDist = 1f
    // These matrices will be used to move and zoom image
    private var mSavedMatrix = Matrix()
    private var mMatrix = Matrix()
    private var painStroke = DEFAULT_STROKE


    companion object {
        const val DEFAULT_STROKE = 25f
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2

        private const val MIN_ZOOM = 3f
        private const val MAX_ZOOM = 10f

        private const val PAINT_COLOR = "#66265c88"
    }


    var scaleFactor : Float = 3f
    private var mScaling = false

    internal inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {

            lastFocusX = detector?.focusX!!
            lastFocusY = detector.focusY

            mScaling = true
            return super.onScaleBegin(detector)
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {


            scaleFactor = detector.scaleFactor
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM))
            val transformationMatrix = Matrix()
            val focusX = detector.focusX
            val focusY = detector.focusY

            //Zoom focus is where the fingers are centered,
            transformationMatrix.postTranslate(-focusX, -focusY)

            transformationMatrix.postScale(scaleFactor, scaleFactor)

            /* Adding focus shift to allow for scrolling with two pointers down. Remove it to skip this functionality. This could be done in fewer lines, but for clarity I do it this way here */
            //Edited after comment by chochim
            val focusShiftX = focusX - lastFocusX
            val focusShiftY = focusY - lastFocusY
            transformationMatrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY)
            paintMatrix.postConcat(transformationMatrix)
            lastFocusX = focusX
            lastFocusY = focusY
            invalidate()

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            mScaling = false
            super.onScaleEnd(detector)
        }
    }

    init {
        mMatrix.setTranslate(1f, 1f)
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener(this)

        mGestureDetector = GestureDetectorCompat(context, this)
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun enableEraseMode() {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun enableDrawMode() {
        paint.xfermode = null
        paint.alpha = 0xFF
    }

    fun setNewImage(alteredBitmap: Bitmap, bmp: Bitmap) {
        canvas = Canvas(alteredBitmap)
        paint = Paint()
        paint.color = Color.parseColor(PAINT_COLOR)
        paint.strokeWidth = painStroke
        paintMatrix = Matrix()
        canvas.drawBitmap(bmp, paintMatrix, paint)

        setImageBitmap(alteredBitmap)
    }


    override fun onScroll(
        event1: MotionEvent,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        mSavedMatrix.postTranslate(-distanceX, -distanceY)
        invalidate()
        return true
    }

    override fun onShowPress(event: MotionEvent) {
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }

    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onFling(
        event1: MotionEvent,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(event: MotionEvent) {
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        mScaleGestureDetector?.onTouchEvent(event)

        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //For Zoom
                mSavedMatrix.set(paintMatrix)
                start.set(event.x, event.y)

                mode = DRAG

                //Draw
                downx = getPointerCoordinates(event)[0]
                downy = getPointerCoordinates(event)[1]
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)

                if (oldDist > 10f) {
                    mSavedMatrix.set(paintMatrix)
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
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                mScaling = true
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                upx = getPointerCoordinates(event)[0]
                upy = getPointerCoordinates(event)[1]
                canvas.drawLine(downx, downy, upx, upy, paint)
                invalidate()
                downx = upx
                downy = upy
            } else if (mode == ZOOM) {
                if(mScaling) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        paintMatrix.set(mSavedMatrix)
                        val scale = newDist / oldDist
                        painStroke = DEFAULT_STROKE.div(scale)
                        paint.strokeWidth = painStroke
                        paintMatrix.postScale(scale, scale, mid.x, mid.y)
                    }
                }else if (event.pointerCount <= 1) {
                    upx = getPointerCoordinates(event)[0]
                    upy = getPointerCoordinates(event)[1]
                    canvas.drawLine(downx, downy, upx, upy, paint)
//                    canvas.drawOval(downx, downy, upx, upy, paint)
                    invalidate()
                    downx = upx
                    downy = upy
                }
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

    private fun getPointerCoordinates(e: MotionEvent): FloatArray {
        val index = e.actionIndex
        val coordinates = floatArrayOf(e.getX(index), e.getY(index))
        val matrix = Matrix()
        imageMatrix.invert(matrix)
        matrix.postTranslate(scrollX.toFloat(), scrollY.toFloat())
        matrix.mapPoints(coordinates)
        return coordinates
    }
}