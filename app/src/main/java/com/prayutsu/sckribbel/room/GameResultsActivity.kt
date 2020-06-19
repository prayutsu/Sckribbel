package com.prayutsu.sckribbel.room

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.ResultsItem
import com.prayutsu.sckribbel.play.GameActivity.Companion.playersList
import com.prayutsu.sckribbel.play.LeaderboardItem
import com.prayutsu.sckribbel.room.RoomActivity.Companion.ROOM_CODE
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_game_results.*
import kotlinx.android.synthetic.main.activity_start_game.*

class GameResultsActivity : AppCompatActivity() {
    private var _doubleBackToExitPressedOnce = false
    lateinit var mediaPlayer: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_results)
        supportActionBar?.title = "LeaderBoard"

        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);

        val right_anim = AnimationUtils.loadAnimation(this, R.anim.right_animation)
        val left_anim = AnimationUtils.loadAnimation(this, R.anim.left_animation)

        logo_results.animation = left_anim
        doodle_results.animation = right_anim
        gif_imageview.animation = left_anim
//        val roomCreatorUser: User? = intent.getParcelableExtra(USER_KEY_SIGNUP)
        val roomCode = intent.getStringExtra(ROOM_CODE).toString()

//        roomcode_textview.text = roomCode
        val db = Firebase.firestore


        val adapter = GroupAdapter<GroupieViewHolder>()
        game_results_recyclerview.adapter = adapter
        game_results_recyclerview.scheduleLayoutAnimation()

        var rank = 0
        for (player in playersList) {
            ++rank
            val item = ResultsItem(player, rank)
            adapter.add(item)
        }
        val roomRef = db.collection("rooms").document(roomCode)
        roomRef
            .delete()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
        mediaPlayer.release()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);
        mediaPlayer.start()
    }

    override fun onBackPressed() {
        Log.i("Back pressed", "onBackPressed--")
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this._doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press again to quit", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }
}
