package com.prayutsu.sckribbel.play

import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.firebase.database.*
import com.google.firebase.database.collection.LLRBNode
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class PaintView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    var width: Int? = 150
    var height: Int? = 150

    private var scale = 1f
    var chosenColor = Color.BLACK
    var chosenWidth = 10f
    lateinit var roomCodePaintView: String

    private var previousX = 0f
    private var previousY = 0f
    val paint: Paint = Paint()
    private val bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var bitmap: Bitmap? = null
    var canvas: Canvas? = null
    private val path: Path = Path()
    private var currentDrawSegment: DrawSegment? = null
    var ref: DatabaseReference? = null
    var allowDraw = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scale = Math.min(1.0f * w / width!!, 1.0f * h / height!!)
        width = w
        height = h
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
        addDatabaseListeners()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bitmap!!, 0f, 0f, bitmapPaint)
        canvas.drawPath(path, paint)
    }

    fun clearDrawing() {
        isDrawingCacheEnabled = false
        width?.let { height?.let { it1 -> onSizeChanged(it, it1, width!!, height!!) } }
        invalidate()
        isDrawingCacheEnabled = true
    }

    private fun touchStart(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        previousX = x
        previousY = y
        currentDrawSegment = DrawSegment()
        currentDrawSegment!!.addPoint(previousX.toInt(), previousY.toInt())
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - previousX)
        val dy = Math.abs(y - previousY)
        if (dx >= 0.0 || dy >= 0.0) {
            path.quadTo(previousX, previousY, (x + previousX) / 2, (y + previousY) / 2)
            previousX = x
            previousY = y
            currentDrawSegment!!.addPoint(previousX.toInt(), previousY.toInt())
        }
    }

    private fun touchUp() {
        path.lineTo(previousX, previousY)
        canvas!!.drawPath(path, paint)
        path.reset()
//        val sColor: Int = paint.color
//        val cWidth: Float = paint.strokeWidth
        saveDrawinf().execute(currentDrawSegment)

//        val segment = DrawSegment()
//        segment.addColor(sColor)
//        segment.addStrokeWidth(cWidth)
//        for (point in currentDrawSegment!!.points) {
//            segment.addPoint((point.x), (point.y))
//
//        }

//        val drawId = UUID.randomUUID().toString().substring(0, 15)
//
//        val db = FirebaseDatabase.getInstance()
//
//        val keyRef = db.getReference("games/$roomCodePaintView/drawing/$drawId")
//        Log.d("MainActivity", "Saving segment to firebase")
//        keyRef
//            .setValue(segment)
    }

    inner class TouchEvent : AsyncTask<String, String, String>(), View.OnTouchListener {
        override fun doInBackground(vararg params: String?): String {
            return "kjd"
        }


        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (allowDraw) {
                val x = event?.x
                val y = event?.y
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
//                    Log.d("Main", "User touched!")
                        if (x != null && y != null) {
                            touchStart(x, y)

                        }
                        invalidate()
                    }
                    MotionEvent.ACTION_MOVE -> {
//                    Log.d("Main", "User is moving finger!")
                        if (x != null && y != null) {
                            touchMove(x, y)
                        }
                        invalidate()
                    }
                    MotionEvent.ACTION_UP -> {
//                    Log.d("Main", "User lifted finger up!")
                        touchUp()
                        invalidate()
                    }
                }
            }
            return true
        }

    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (allowDraw) {
//            val x = event.x
//            val y = event.y
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
////                    Log.d("Main", "User touched!")
//                    touchStart(x, y)
//                    invalidate()
//                }
//                MotionEvent.ACTION_MOVE -> {
////                    Log.d("Main", "User is moving finger!")
//                    touchMove(x, y)
//                    invalidate()
//                }
//                MotionEvent.ACTION_UP -> {
////                    Log.d("Main", "User lifted finger up!")
//                    touchUp()
//                    invalidate()
//                }
//            }
//        }
//        return true
//    }

    fun addDatabaseListeners() {
        ref!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val segment = dataSnapshot.getValue(DrawSegment::class.java)


                val sColor = segment?.color
                val sWidth = segment?.strokeWidth
                if (sColor != null) {
                    paint.color = sColor
                }
                paint.strokeWidth = sWidth ?: return

                val task = ShowDrawing()

                task.execute(segment)
//                val path = getPathForPoints(segment!!.points, scale)
//                canvas!!.drawPath(path, paint)
//                invalidate()
                Log.d("Fetch", "Drawing fetching")

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                clearDrawing()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun clearDrawingForAll() {
        ref!!.removeValue()
        paint.color = chosenColor
        paint.strokeWidth = chosenWidth
    }

    fun changeStrokeColor(sColor: Int) {
        paint!!.color = sColor
        chosenColor = sColor
    }

    fun eraser() {
        paint!!.color = Color.WHITE
    }

    fun setStrokeWidth(progress: Int) {
        var strokeWidth: Float = (progress / 2).toFloat()
        paint.strokeWidth = strokeWidth
        chosenWidth = strokeWidth
    }

    inner class saveDrawinf : AsyncTask<DrawSegment, Int, DrawSegment>() {
        override fun doInBackground(vararg params: DrawSegment?): DrawSegment {
            val segment = DrawSegment()
            for (point in params[0]!!.points) {
                segment.addPoint((point.x), (point.y))
            }
            return segment
        }

        override fun onPostExecute(result: DrawSegment?) {
            super.onPostExecute(result)
            if (result != null) {
                result.addColor(paint.color)
                result.addStrokeWidth(paint.strokeWidth)
                val drawId = UUID.randomUUID().toString().substring(0, 15)

                val db = FirebaseDatabase.getInstance()

                val keyRef = db.getReference("games/$roomCodePaintView/drawing/$drawId")
                Log.d("MainActivity", "Saving segment to firebase")
                keyRef
                    .setValue(result)
            }
        }
    }

    inner class ShowDrawing : AsyncTask<DrawSegment, String, Path>() {
        override fun doInBackground(vararg params: DrawSegment?): Path {
            val path = getPathForPoints(params[0]!!.points, scale)
            Log.d("AsyncTask doinbg", "doInBackground: ")
            return path
        }

        override fun onPostExecute(result: Path?) {
            super.onPostExecute(result)
            Log.d("AsyncTask onpost execute", "onPostExecute: ")
            if (result != null) {
                canvas!!.drawPath(result, paint)
            }
            invalidate()
        }

    }

    companion object {
        fun getPathForPoints(points: List<Point>, scale: Float): Path {
            val path = Path()
            var current = points[0]
            path.moveTo(
                (scale * current!!.x).toFloat(),
                (scale * current.y).toFloat()
            )
            var next: Point? = null
            for (i in 1 until points.size) {
                next = points[i]
                path.quadTo(
                    Math.round(scale * current!!.x).toFloat(),
                    Math.round(scale * current.y).toFloat(),
                    Math.round(scale * (next!!.x + current.x) / 2).toFloat(),
                    Math.round(scale * (next.y + current.y) / 2)
                        .toFloat()
                )
                current = next
            }
            if (next != null) {
                path.lineTo(
                    Math.round(scale * next.x).toFloat(),
                    Math.round(scale * next.y).toFloat()
                )
            }
            return path
        }
    }


    init {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }
}