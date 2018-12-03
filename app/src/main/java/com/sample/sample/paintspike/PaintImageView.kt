package com.sample.sample.paintspike

import android.content.Context
import android.graphics.*
import android.widget.ImageView
import android.util.AttributeSet
import android.graphics.PointF
import android.support.v4.view.GestureDetectorCompat
import android.view.*
import android.view.MotionEvent

//https://stackoverflow.com/questions/40033801/how-do-i-only-erase-the-draw-path-not-the-image-on-the-canvas-on-android
//Erase

//https://stackoverflow.com/questions/11114625/android-canvas-redo-and-undo-operation
//https://stackoverflow.com/questions/19418878/implementing-pinch-zoom-and-drag-using-androids-build-in-gesture-listener-and-s
//https://stackoverflow.com/questions/26452574/android-zooming-with-two-fingers-ontouch-and-setscalex-setscaley

class PaintImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr),
    GestureDetector.OnGestureListener,
    IPaintView{

    /**
     * Arrays to undo what had been painted with their respective
     * paint brushes for stroke conservation
     */
    private lateinit var mLastPath: Path
    private var mPaint: Paint = Paint()

    private val paths = ArrayList<Path>()
    private val paints = ArrayList<Paint>()
    private val undonePaths = ArrayList<Path>()

    /**
     * Gestures to Zoom and Drag canvas matrix
     */
    private var mGestureDetector : GestureDetectorCompat? = null
    private var mScaleGestureDetector: ScaleGestureDetector? = null

    private var downx = 0f
    private var downy = 0f
    private var upx = 0f
    private var upy = 0f

    var lastFocusX: Float = 0f
    var lastFocusY: Float = 0f

    private lateinit var mCanvas: Canvas
    private var paintMatrix: Matrix = Matrix()

    private var mode = NONE

    private var start = PointF()
    private var mid = PointF()
    private var oldDist = 1f
    // These matrices will be used to move and zoom image
    private var mSavedMatrix = Matrix()
    private var painStroke = DEFAULT_STROKE

    var scaleFactor : Float = 3f
    private var mScaling = false

    companion object {
        const val DEFAULT_STROKE = 25f
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2

        private const val MIN_ZOOM = 3f
        private const val MAX_ZOOM = 5f

        private const val PAINT_COLOR = "#66265c88"
    }

    init {
        imageMatrix = Matrix()
        imageMatrix.setTranslate(1f, 1f)
        scaleType = ScaleType.MATRIX

        createPaint()

        mGestureDetector = GestureDetectorCompat(context, this)
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

        isFocusable = true
        isFocusableInTouchMode = true

        getNewPathPen()
    }

    private var mEraseMode = false
    private fun createPaint(){
        mPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.style = Paint.Style.STROKE
        if(mEraseMode){
            mPaint.color = Color.parseColor("#FFFFFFFF")
            //mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }else{
            mPaint.color = Color.parseColor(PAINT_COLOR)
            //mPaint.xfermode = null
        }
        mPaint.strokeWidth = painStroke
        mPaint.strokeJoin = Paint.Join.MITER
        mPaint.strokeCap = Paint.Cap.ROUND
    }

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

    override fun enableEraseMode() {
        mEraseMode = true
        //mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        //mCanvas.drawColor(0, PorterDuff.Mode.CLEAR)

        //mPaint.color = Color.parseColor("#FFFFFFFF")
    }

    override fun enableDrawMode() {
        mEraseMode = false
        //mPaint.xfermode = null
    }

    private lateinit var mRealBitmap : Bitmap
    fun setNewImage(alteredBitmap: Bitmap, bmp: Bitmap) {
        mRealBitmap = alteredBitmap
        mCanvas = Canvas(alteredBitmap)
        mCanvas.drawBitmap(bmp, imageMatrix, mPaint)

        setImageBitmap(alteredBitmap)
    }

    fun restoreBitmap(){
        imageMatrix = Matrix()
        imageMatrix.setTranslate(1f, 1f)
        mCanvas = Canvas(mRealBitmap)
        mCanvas.drawBitmap(mRealBitmap, imageMatrix, mPaint)
        setImageBitmap(mRealBitmap)
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


    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector?.onTouchEvent(event)
        mScaleGestureDetector?.onTouchEvent(event)

        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> actionDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> actionPointerDown(event)
            MotionEvent.ACTION_UP -> actionUp(event)
            MotionEvent.ACTION_POINTER_UP -> actionPointerUp()
            MotionEvent.ACTION_MOVE -> move(event)
        }


        //Paint sample
        /*
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                getNewPathPen()
                mLastPath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                mLastPath.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
            }
        }*/
        //invalidate()
        imageMatrix = paintMatrix
        invalidate()
        return true
    }

    private fun move(event : MotionEvent){
        if (mode == DRAG) {
            upx = getPointerCoordinates(event)[0]
            upy = getPointerCoordinates(event)[1]
            //mCanvas.drawLine(downx, downy, upx, upy, mPaint)

            mLastPath.lineTo(event.x, event.y)
            //mLastPath.lineTo(downx, downy)

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
                    mPaint.strokeWidth = painStroke
                    paintMatrix.postScale(scale, scale, mid.x, mid.y)
                }
            }
        }
    }

    private fun actionUp(event : MotionEvent){
        val xDiff = Math.abs(event.x - start.x).toInt()
        val yDiff = Math.abs(event.y - start.y).toInt()
        if (xDiff < 8 && yDiff < 8) {
            performClick()
        }
        mode = NONE
    }

    private fun actionDown(event: MotionEvent){
        getNewPathPen()
        mLastPath.moveTo(event.x, event.y)

        //For Zoom
        mSavedMatrix.set(paintMatrix)
        start.set(event.x, event.y)

        mode = DRAG

        //Draw
        downx = getPointerCoordinates(event)[0]
        downy = getPointerCoordinates(event)[1]
    }

    private fun actionPointerUp(){
        mode = NONE
        mScaling = true
    }

    private fun actionPointerDown(event: MotionEvent){
        oldDist = spacing(event)
        if (oldDist > 10f) {
            mSavedMatrix.set(paintMatrix)
            midPoint(mid, event)
            mode = ZOOM
        }
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

    override fun onUndoDraw() {
        //restoreBitmap()
        if (paths.size > 0) {
            undonePaths.add(paths.removeAt(paths.size-1))
            invalidate()
        }
    }

    /**
     * With Path
     */
     override fun onDraw(canvas: Canvas) {
         super.onDraw(canvas)
         for (i in paths.indices) {

             //draw depending on scroll and drag
             val p =  Path()
             p.addPath(paths[i], imageMatrix)

             //paths[i].transform(paintMatrix)
             canvas.drawPath(p, paints[i])
         }
     }

    private fun getNewPathPen() {
        mLastPath = Path()
        paths.add(mLastPath)
        val paint = Paint()
        mPaint = paint
        createPaint()
        paints.add(paint)
    }
}