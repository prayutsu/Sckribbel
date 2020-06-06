package com.prayutsu.sckribbel.play

import android.graphics.Color
import android.graphics.Point
import java.util.*

class DrawSegment {
    var points: ArrayList<Point>
    var color: Int = 0
    fun addPoint(x: Int, y: Int) {
        points.add(Point(x, y))
    }

    fun addColor(sColor: Int) {
        color = sColor
    }

    init {
        points = ArrayList()
    }
}