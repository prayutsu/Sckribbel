package com.prayutsu.sckribbel.room

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.play.GameActivity
import com.prayutsu.sckribbel.play.Player
import com.prayutsu.sckribbel.play.PlayerItem
import com.prayutsu.sckribbel.register.SignupActivity
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity.Companion.players
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_start_game.*
import kotlinx.android.synthetic.main.activity_start_join.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.LineNumberReader

class StartJoinActivity : AppCompatActivity() {

    companion object {
        const val JOIN_USER_KEY = "JOIN_USER_KEY"
        const val TAG = "Join Game"
    }

    private var _doubleBackToExitPressedOnce = false

    var myUser: User? = null
    var fetching: ListenerRegistration? = null
    lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_join)
        supportActionBar?.title = "Play Do-odle"
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);
        val roomCodeReceived = intent.getStringExtra(RoomActivity.ROOM_CODE).toString()
        myUser = intent.getParcelableExtra(SignupActivity.USER_KEY_SIGNUP)

        createArrayOfWords(roomCodeReceived)
        startGame(roomCodeReceived)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.stop()
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

    private fun createArrayOfWords(roomCode: String) {
        var mWord: String = ""
        val am = this.assets
        //open file using asset manager
        val inputStraem = am.open("words.txt")
        //read buffer manager
        val reader = BufferedReader(InputStreamReader(inputStraem))
        //Important: use of LineNumberReader Class
        val lnr = LineNumberReader(reader)
//        val r = Random()
//        val n = r.nextInt(91) + 1
//        lnr.lineNumber = n
        for (i in 1..6801) {
            mWord = lnr.readLine()
            WordsArray.words.add(mWord)
            if (i == 6801)
                fetchPlayers(roomCode)
        }
        Log.d("MyLog", "The letter is $mWord")
    }

    private fun fetchPlayers(roomCode: String) {

        val db = Firebase.firestore

        val userRef = db.collection("rooms").document(roomCode)
            .collection("players")

        fetching =
            userRef
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        Log.w("Fetch", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    val adapter = GroupAdapter<GroupieViewHolder>()
                    for (doc in value!!) {
                        val user = doc.toObject(User::class.java)
                        adapter.add(PlayerItem(user))
                        Log.d("Fetch", "Document is: ${doc.data}")
                    }
                    join_game_recycler_view.adapter = adapter
                    join_game_recycler_view.addItemDecoration(
                        DividerItemDecoration(
                            this,
                            DividerItemDecoration.VERTICAL
                        )
                    )
                }
    }

    private fun makeListOfPlayers(roomCode: String) {
        val db = Firebase.firestore
        val playerRefInGame = db.collection("rooms").document(roomCode)
            .collection("game")

        var fetchedPlayer = Player("jvvh", " n")
        playerRefInGame
            .whereEqualTo("hasAlreadyGuessed", false)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(StartGameActivity.TAG, "${document.id} => ${document.data}")
                    fetchedPlayer.username = document.getString("username").toString()
                    fetchedPlayer.profileImageUrl = document.getString("profileImageUrl").toString()
                    fetchedPlayer.indexOfTurn = (document.get("indexOfTurn") as Long).toInt()
                    fetchedPlayer.currentDrawer = document.get("currentDrawer") as Boolean
                    fetchedPlayer.hasAlreadyGuessed = document.get("hasAlreadyGuessed") as Boolean
                    fetchedPlayer.points = (document.get("points") as Long).toInt()
                    players.add(fetchedPlayer)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(StartGameActivity.TAG, "Error getting documents: ", exception)
            }

    }


    private fun startGame(roomCode: String) {
        val db = Firebase.firestore
        val isGameStartedRef = db.collection("rooms").document(roomCode)

        isGameStartedRef
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    var key: Boolean = snapshot.get("isGameStarted") as Boolean
                    if (key) {
                        makeListOfPlayers(roomCode)
                        val intent = Intent(this, GameActivity::class.java)
                        key = false
                        intent.putExtra(JOIN_USER_KEY, roomCode)
                        intent.putExtra(USER_KEY_SIGNUP, myUser)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                    }

                } else {
                    Log.d(TAG, "Current data: null")
                }
            }

    }
}
