package com.sample.sample.paintspike

import android.content.Context
import android.graphics.*
import android.widget.ImageView
import android.util.AttributeSet
import android.graphics.PointF
import android.support.v4.view.GestureDetectorCompat
import android.view.*
import android.view.MotionEvent
import android.graphics.Bitmap



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
    private var mPainter: Painter = Painter()
    private var mImagePainter: Painter = Painter()

    private val paths = ArrayList<Path>()
    private val paints = ArrayList<Painter>()
    private val undonePaths = ArrayList<Path>()
    private val undonePaints = ArrayList<Paint>()

    /**
     * Gestures to Zoom and Drag canvas matrix
     */
    private var mGestureDetector : GestureDetectorCompat? = null
    //private var mScaleGestureDetector: ScaleGestureDetector? = null

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
    private var mScrollAndDragMatrix = Matrix()

    var scaleFactor : Float = 1f

    private var mEraseMode = false

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        const val BRUSH_STROKE_RATION = 10f

        private const val MIN_ZOOM = 1f
        private const val MAX_ZOOM = 10f

        private const val PAINT_COLOR = "#66265c88"
    }

    init {
        imageMatrix = Matrix()
        imageMatrix.setTranslate(1f, 1f)
        scaleType = ScaleType.MATRIX
        mGestureDetector = GestureDetectorCompat(context, this)
        //mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        isFocusable = true
        isFocusableInTouchMode = true
        getNewPathPen()
    }



    /*internal inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {

            lastFocusX = detector?.focusX!!
            lastFocusY = detector.focusY

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
            val focusShiftX = focusX - lastFocusX
            val focusShiftY = focusY - lastFocusY
            matrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY)
            transformationMatrix.postTranslate(focusX + focusShiftX, focusY + focusShiftY)
            paintMatrix.postConcat(transformationMatrix)
            lastFocusX = focusX
            lastFocusY = focusY
            invalidate()

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
        }
    }*/

    private var mScaling = false
    override fun enableEraseMode() {
        if(!mEraseMode){
            getNewPathPen()
        }
        mEraseMode = true
    }

    override fun enableDrawMode() {
        if(mEraseMode){
            getNewPathPen()
        }
        mEraseMode = false
    }

    private lateinit var mRealBitmap : Bitmap
    fun setNewImage(alteredBitmap: Bitmap, bmp: Bitmap) {
        mRealBitmap = alteredBitmap
        /*mCanvas = Canvas(alteredBitmap)
        mCanvas.drawBitmap(bmp, imageMatrix, paint)

        setImageBitmap(alteredBitmap)*/
        val canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(canvasBitmap)
        invalidate()
        /*mCanvas = Canvas()
        val mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas.setBitmap(mBitmap)*/
    }

    /*fun restoreBitmap(){
        imageMatrix = Matrix()
        imageMatrix.setTranslate(1f, 1f)
        mCanvas = Canvas(mRealBitmap)
        mCanvas.drawBitmap(mRealBitmap, imageMatrix, paint)
        setImageBitmap(mRealBitmap)
    }*/

    override fun onScroll(
        event1: MotionEvent,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        mScrollAndDragMatrix.postTranslate(-distanceX, -distanceY)
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
        //mScaleGestureDetector?.onTouchEvent(event)
        mGestureDetector?.onTouchEvent(event)


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
        mScaling = false
        if (mode == DRAG) {
            upx = getPointerCoordinates(event)[0]
            upy = getPointerCoordinates(event)[1]
            //mCanvas.drawLine(downx, downy, upx, upy, paint)

            //mLastPath.lineTo(event.x, event.y)
            mLastPath.lineTo(downx, downy)

            invalidate()
            downx = upx
            downy = upy
        } else if (mode == ZOOM) {
                val newDist = spacing(event)
                if (newDist > 10f) {
                    mScaling = true
                    paintMatrix.set(mScrollAndDragMatrix)
                    val scale = newDist / oldDist
                    scaleFactor = scale
                    paintMatrix.postScale(scale, scale, mid.x, mid.y)
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

        //For Zoom
        mScrollAndDragMatrix.set(paintMatrix)
        start.set(event.x, event.y)

        mode = DRAG

        //Draw
        downx = getPointerCoordinates(event)[0]
        downy = getPointerCoordinates(event)[1]
        //mLastPath.moveTo(event.x, event.y)
        //Second One
        mLastPath.moveTo(downx, downy)
        //Original
        //mLastPath.moveTo(downx.times(mid.x), downy.times(mid.y))
    }

    private fun actionPointerUp(){
        mode = NONE
    }

    private fun actionPointerDown(event: MotionEvent){
        oldDist = spacing(event)
        if (oldDist > 10f) {
            mScrollAndDragMatrix.set(paintMatrix)
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
        if (paths.size > 0 ) {
            /*
            //TODO For redo
            undonePaths.add(paths.removeAt(paths.size-1))
            undonePaints.add(paints.removeAt(paints.size-1))*/
            paths.removeAt(paths.size-1)
            paints.removeAt(paints.size-1)
            if(paints.size > 0) {
                mPainter = paints[paints.size - 1]
            }
            invalidate()
        }
    }


    private var currentStroke = Painter.DEFAULT_STROKE
    fun increaseStrokeSize(){
        currentStroke += BRUSH_STROKE_RATION
    }

    fun decreaseStrokeSize(){
        if(currentStroke > Painter.DEFAULT_STROKE) {
            currentStroke -= BRUSH_STROKE_RATION
        }
    }

    fun setDefaultStroke(){
        currentStroke = Painter.DEFAULT_STROKE
    }
    /**
     * With Path
     */
     override fun onDraw(canvas: Canvas) {
         super.onDraw(canvas)

        canvas.save()
        //canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        //canvas.translate(scrollX.toFloat(), scrollY.toFloat())
        //matrix.postTranslate(mid.x, mid.y)
        //canvas.scale(scaleFactor, scaleFactor)
        //clipBounds = canvas.clipBounds
        /*if (::mRealBitmap.isInitialized) {

            canvas.drawBitmap(mRealBitmap, 0f, 0f, paint)
        }
        setPaintValue(paint)*/

        // first update the canvas bitmap
        //setPaintValue(mPaint)
        for (i in paths.indices) {

            //draw depending on scroll and drag
            val p =  Path()
            p.addPath(paths[i], paintMatrix)
            p.transform(paintMatrix)
            canvas.drawPath(p, paints[i].paint)
        }
        // then draw it on top of the image
        //setDefaultPaint()
        if(::mRealBitmap.isInitialized) {
            //with screen canvas proportios
            //canvas.drawBitmap(mRealBitmap, null, destImageRect, paint)
            //with image proportions :
            canvas.drawBitmap(mRealBitmap, paintMatrix, mImagePainter.paint)
        }
        //setPaintValue(mPaint)
        canvas.restore()


/*
         for (i in paths.indices) {

             //draw depending on scroll and drag
             val p =  Path()
             p.addPath(paths[i], imageMatrix)

             /*if(scaleFactor <= 1) {
                 paints[i].strokeWidth = scaleFactor?.times(DEFAULT_STROKE)
             }*/
             //paths[i].transform(paintMatrix)
             canvas.drawPath(p, paints[i])
         }*/
     }

    private fun getNewPathPen() {
        mLastPath = Path()
        paths.add(mLastPath)
        mPainter = Painter()
        mPainter.changeStrokeTo(currentStroke)
        mPainter.setEraseMode(mEraseMode)
        paints.add(mPainter)
    }

    private lateinit var destImageRect : Rect
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //when size changed, store the new size
        destImageRect = Rect(0, 0, w, h)
    }
}