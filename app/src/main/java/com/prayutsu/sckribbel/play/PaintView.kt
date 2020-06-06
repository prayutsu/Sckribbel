package com.prayutsu.sckribbel.play

import android.content.Context
import android.graphics.*
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
    var width: Int? = 100
    var height: Int? = 100

    private var scale = 1f
    var chosenColor = Color.BLACK

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
        val sColor: Int = paint.color
        val segment = DrawSegment()
        for (point in currentDrawSegment!!.points) {
            segment.addPoint((point.x), (point.y))
            segment.addColor(sColor)
        }

        val drawId = UUID.randomUUID().toString().substring(0, 15)


        val db = FirebaseDatabase.getInstance()

        val keyRef = db.getReference("games/$roomCodePaintView/drawing/$drawId")
        Log.d("MainActivity", "Saving segment to firebase")
        keyRef
            .setValue(segment)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allowDraw) {
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("Main", "User touched!")
                    touchStart(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d("Main", "User is moving finger!")
                    touchMove(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("Main", "User lifted finger up!")
                    touchUp()
                    invalidate()
                }
            }
        }
        return true
    }

    fun addDatabaseListeners() {
        ref!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val segment = dataSnapshot.getValue(DrawSegment::class.java)
                val sColor = segment?.color
                if (sColor != null) {
                    paint.color = sColor
                }
                canvas!!.drawPath(getPathForPoints(segment!!.points, scale.toDouble()), paint)
                Log.d("Fetch", "Drawing fetching")
                invalidate()
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
    }

    fun changeStrokeColor(sColor: Int) {
        paint!!.color = sColor
        chosenColor = sColor
    }

    fun eraser() {
        paint!!.color = Color.WHITE
    }


    companion object {
        fun getPathForPoints(points: List<Point?>, scale: Double): Path {
            val path = Path()
            var current = points[0]
            path.moveTo(
                Math.round(scale * current!!.x).toFloat(),
                Math.round(scale * current.y).toFloat()
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
        paint.strokeWidth = 12f
    }
}