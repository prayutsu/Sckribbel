package com.prayutsu.sckribbel.room

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.ClipboardManager
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import java.io.*
import java.util.*


class StartGameActivity : AppCompatActivity() {
    companion object {
        val TAG = "Game Started"
    }

    var myUser: User? = null
    private var numQuery: ListenerRegistration? = null
    private var fetching: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_game)

        val roomCodeReceived = intent.getStringExtra(ROOM_CODE).toString()
        myUser = intent.getParcelableExtra(USER_KEY_SIGNUP)

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
//            takeScreenshot(roomCodeReceived)
            val imageUri = Uri.parse(
                "android.resource://" + packageName
                        + "/drawable/" + "signn_min2"
            )
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, "$roomCodeReceived")
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
            shareIntent.type = "image/*"
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "send"))

        }


        start_game_button.setOnClickListener {
            numQuery?.remove()
            startGame(roomCodeReceived)
        }

    }

    private fun takeScreenshot(roomCode: String) {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath: String =
                Environment.getExternalStorageDirectory().toString().toString() + "/" + now + ".jpg"

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            val imageFile = File(mPath)
            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            send(imageFile, roomCode)
        } catch (e: Throwable) {
            // Several error may come out with file handling or DOM
            e.printStackTrace()
        }
    }

    fun screenShot(view: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
        return bitmap
    }


    private fun send(imageFile: File, roomCode: String) {
//        val uri = Uri.fromFile(imageFile)
//        val shareIntent = Intent()
//        shareIntent.action = Intent.ACTION_SEND
//        shareIntent.putExtra(Intent.EXTRA_TEXT, "roomcode is $roomCode")
//        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
//        shareIntent.type = "image/*"
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        startActivity(Intent.createChooser(shareIntent, "$roomCode"))
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val uri = Uri.fromFile(imageFile)
        intent.setDataAndType(uri, "image/*")
        startActivity(intent)
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
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        fetching?.remove()
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(USER_KEY_SIGNUP, myUser)
        intent.putExtra(JOIN_USER_KEY, roomCodeReceived)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);

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
                            start_game_button.visibility = View.VISIBLE
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


            }


    }


}


