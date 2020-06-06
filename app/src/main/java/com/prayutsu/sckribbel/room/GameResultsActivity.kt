package com.prayutsu.sckribbel.room

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

class GameResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_results)
//
//        val roomCreatorUser: User? = intent.getParcelableExtra(USER_KEY_SIGNUP)
        val roomCode = intent.getStringExtra(ROOM_CODE).toString()

//        roomcode_textview.text = roomCode
//        val db = Firebase.firestore
//        val roomRef = db.collection("rooms").document(roomCode)
//        roomRef
//            .delete()
//
        val adapter = GroupAdapter<GroupieViewHolder>()
        game_results_recyclerview.adapter = adapter

        val db = Firebase.firestore
        val ref = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers")

        var rank = 0
        for (player in playersList) {
            ++rank
            val item = ResultsItem(player, rank)
            adapter.add(item)
        }
    }
}
