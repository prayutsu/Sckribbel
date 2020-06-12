package com.prayutsu.sckribbel.play

import com.prayutsu.sckribbel.R
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.SeekBar
import kotlinx.android.synthetic.main.increase_stroke.*
import com.prayutsu.sckribbel.play.GameActivity.Companion.myPaintView
import com.prayutsu.sckribbel.play.GameActivity.Companion.seekProgress


class DialogStroke
    (var c: Activity) : Dialog(c), View.OnClickListener {
    var d: Dialog? = null
    var progressDetected: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.increase_stroke)
        set_textview.setOnClickListener(this)
        cancel_textview.setOnClickListener(this)

        stroke_seekbar.progress = seekProgress
        stroke_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressDetected = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.set_textview -> {
                myPaintView?.setStrokeWidth(progressDetected)
                seekProgress = progressDetected
            }
            R.id.cancel_textview -> {
                dismiss()
            }
            else -> {
                dismiss()
            }
        }
        dismiss()
    }

}