package com.prayutsu.sckribbel.room

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.MediaStore
import android.text.ClipboardManager
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.play.GameActivity
import com.prayutsu.sckribbel.play.PlayerItem
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity.Companion.ROOM_CODE
import com.prayutsu.sckribbel.room.StartJoinActivity.Companion.JOIN_USER_KEY
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_start_game.*
import kotlinx.android.synthetic.main.activity_start_join.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.LineNumberReader


class StartGameActivity : AppCompatActivity() {
    companion object {
        val TAG = "Game Started"
    }

    private var _doubleBackToExitPressedOnce = false
    lateinit var bottom_anim: Animation
    lateinit var mediaPlayer: MediaPlayer
    var myUser: User? = null
    private var numQuery: ListenerRegistration? = null
    private var fetching: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_game)
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);
        supportActionBar?.title = "Play Do-odle"

        val roomCodeReceived = intent.getStringExtra(ROOM_CODE).toString()
        myUser = intent.getParcelableExtra(USER_KEY_SIGNUP)

        bottom_anim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        roomcode_textview.animation = bottom_anim
        copy_button.animation = AnimationUtils.loadAnimation(this, R.anim.right_animation)
        imageButton_share.animation = AnimationUtils.loadAnimation(this, R.anim.right_animation)

        roomcode_textview.text = roomCodeReceived
        roomcode_textview.visibility = View.VISIBLE
        createArrayOfWords(roomCodeReceived)
        checkAllEntered(roomCodeReceived)
//        fetchPlayers(roomCodeReceived)

        copy_button.setOnClickListener {
            val cm = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = roomcode_textview.text.toString()
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }

        imageButton_share.setOnClickListener {
            val roomCode = roomCodeReceived.toLong()
            try {
                var imageUri = Uri.parse(
                        MediaStore.Images.Media.insertImage(
                            this.contentResolver,
                            BitmapFactory.decodeResource(
                                resources,
                                R.drawable.share_app
                            ), "Play Doodle", "nkdcd ds,mdc "
                        )
                    )

                val text =
                    "I want to play Do-odle with you!\nSign up if you haven't and join room " +
                            "by entering the Room Code \"$roomCode\".\n" +
                            "Believe me this is awesome !!"
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_TEXT, text)
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                shareIntent.putExtra(Intent.EXTRA_TITLE, "Share roomCode via ..")
                shareIntent.type = "image/*"

                startActivity(shareIntent)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    "cnkjdnj",
                    Toast.LENGTH_LONG
                ).show()
            }

        }


        start_game_button.setOnClickListener {
            start_game_button.isEnabled = false
            progressBar_start_game.visibility = View.VISIBLE
            numQuery?.remove()
            startGame(roomCodeReceived)
        }

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

    private fun startGame(roomCodeReceived: String) {
        val db = Firebase.firestore

        val timerRef = db.collection("rooms").document(roomCodeReceived)
            .collection("game").document("timer")
        val turnRef = db.collection("rooms").document(roomCodeReceived)
            .collection("game").document("turn")
        val roundRef = db.collection("rooms").document(roomCodeReceived)
            .collection("game").document("round")
        val finishTimerRef = db.collection("rooms").document(roomCodeReceived)
            .collection("game").document("finishTimer")
        val numRef = db.collection("rooms").document(roomCodeReceived)
            .collection("game").document("numberGuessed")

        val turnHashmap = hashMapOf("currentTurn" to 1)
        val timerHashmap = hashMapOf("startTimer" to false)
        val roundHashmap = hashMapOf("currentRound" to 1)
        val fmap = hashMapOf("ftimer" to false)
        val countMap = hashMapOf("numGuessed" to 0)

        timerRef
            .set(timerHashmap)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        turnRef
            .set(turnHashmap)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        roundRef
            .set(roundHashmap)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        finishTimerRef
            .set(fmap)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        numRef
            .set(countMap)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

        val gameRef = db.collection("rooms").document(roomCodeReceived)

        gameRef
            .update("isGameStarted", true)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing document", e)
                start_game_button.isEnabled = true
                progressBar_start_game.visibility = View.VISIBLE
            }
        fetching?.remove()
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(USER_KEY_SIGNUP, myUser)
        intent.putExtra(JOIN_USER_KEY, roomCodeReceived)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
//        overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);

    }


    private fun checkAllEntered(roomCode: String) {
        val db = Firebase.firestore
        val ref = db.collection("rooms").document(roomCode)
        var playersJoined = 0
        var maxplayers = -1
        numQuery =
            ref
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Current data: ${snapshot.data}")
                        maxplayers = (snapshot.get("maxPlayers") as Long).toInt()
                        playersJoined = (snapshot.get("playersCount") as Long).toInt()
                        if (maxplayers == playersJoined) {
                            object : CountDownTimer(2000, 1000) {
                                override fun onFinish() {
                                    start_game_button.animation = bottom_anim
                                    start_game_button.visibility = View.VISIBLE
                                }

                                override fun onTick(millisUntilFinished: Long) {
                                }

                            }.start()
                        }
                    } else {
                        Log.d(TAG, "Current data: null")
                    }
                }
    }

    private fun fetchPlayers(roomCode: String) {

        val db = Firebase.firestore

        val userRef: CollectionReference = db.collection("rooms").document(roomCode)
            .collection("players")

        fetching =
            userRef.addSnapshotListener { value, e ->
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
                start_game_recyclerView.adapter = adapter
                start_game_recyclerView.scheduleLayoutAnimation()
                start_game_recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        this,
                        DividerItemDecoration.VERTICAL
                    )
                )


            }


    }


}


