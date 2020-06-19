package com.prayutsu.sckribbel.room


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AlertDialogLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.play.Player
import com.prayutsu.sckribbel.register.SignupActivity
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import kotlinx.android.synthetic.main.activity_room.*


class RoomActivity : AppCompatActivity() {

    companion object {
        val TAG = "Check"
        val ROOM_CODE = "ROOM_CODE"
        var playerPlaying = Player("", "")
        var players = mutableSetOf<Player>()
        lateinit var mediaPlayer: MediaPlayer
        var colorArrayPlayers = listOf<Int>(
            Color.parseColor("#5fd870"),
            Color.parseColor("#eab06b"),
            Color.parseColor("#ec4452"),
            Color.parseColor("#ffeb3b"),
            Color.parseColor("#ff5722"),
            Color.parseColor("#26a69a"),
            Color.parseColor("#8bc34a"),
            Color.parseColor("#ffeb3b"),
            Color.parseColor("#ffc107"),
            Color.parseColor("#00c853"),
            Color.parseColor("#e65100"),
            Color.parseColor("#ec407a"),
            Color.parseColor("#ba68c8"),
            Color.parseColor("#2196f3"),
            Color.parseColor("#E5B908"),
            Color.parseColor("#08E543"),
            Color.parseColor("#E5082D"),
            Color.parseColor("#1AA9C5"),
            Color.parseColor("#E98D25"),
            Color.parseColor("#8610EE"),
            Color.parseColor("#E3E919"),
            Color.parseColor("#59E919")
        )
    }


    private var _doubleBackToExitPressedOnce = false

    var roomUser: User? = null
    var userEntry: ListenerRegistration? = null
    var alreadyJoined = false
    var hasGameStarted = true
    var enteredRoomCode: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
        supportActionBar?.title = "Create or Join A Room"

        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music);
        val fade_in = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        val fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out_slow)
        val top = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        dummy_create_button.animation = fade_in
        join_room_button.animation = fade_in
        logo_imageview.animation = top

        roomUser = intent.getParcelableExtra(USER_KEY_SIGNUP)
        create_room_button.setOnClickListener {
            create_room_button.isEnabled = false
            Log.d("Room", "CREATE button pressed")
            val text = number_of_players_editText.text.toString()
            if (text != "") {
                val numberOfPlayers = (number_of_players_editText.text.toString()).toInt()
                if (numberOfPlayers in 2..6) {
                    room_code_edittext.visibility = View.INVISIBLE
                    createRoom(numberOfPlayers)
                } else {
                    create_room_button.isEnabled = true
                    Toast.makeText(
                        this,
                        "Number of players must be from 2 to 6",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Please enter number of players!", Toast.LENGTH_SHORT).show()
                create_room_button.isEnabled = true
                return@setOnClickListener
            }

        }

        dummy_create_button.setOnClickListener {
            dummy_create_button.visibility = View.INVISIBLE
            number_of_players_editText.visibility = View.VISIBLE
            create_room_button.visibility = View.VISIBLE
            number_of_players_ediitext_layout.visibility = View.VISIBLE

        }

        join_room_button.setOnClickListener {
            room_code_edittext.visibility = View.VISIBLE
            join_room_button.visibility = View.INVISIBLE
            button_final_join.visibility = View.VISIBLE
            room_code_edittext_layout.visibility = View.VISIBLE
        }


        button_final_join.setOnClickListener {
            enteredRoomCode = room_code_edittext.text.toString()
            if (enteredRoomCode != "") {
                button_final_join.isEnabled = false
                tryJoinRoom(enteredRoomCode)
                number_of_players_editText.visibility = View.INVISIBLE
            } else {
                Toast.makeText(this, "Please enter some non-empty roomcode!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun createRoom(maxNumOfPlayers: Int) {

        val db = Firebase.firestore

        val roomCode =
            (kotlin.math.floor(Math.random() * 90000000L).toLong() + 10000000L).toString()

        val data = hashMapOf(
            "playersCount" to 1,
            "isGameStarted" to false, "maxPlayers" to maxNumOfPlayers
        )

        val roomRef = db.collection("rooms").document("$roomCode")
        roomRef
            .set(data)
            .addOnSuccessListener {
                Log.d(
                    StartGameActivity.TAG,
                    "DocumentSnapshot successfully written!"
                )
            }
            .addOnFailureListener { e -> Log.w(StartGameActivity.TAG, "Error writing document", e) }

        val documentRef = db.collection("rooms").document("$roomCode")
            .collection("players")

        documentRef
            .add(roomUser!!)
            .addOnSuccessListener { documentReference ->
                Log.d("Room", "DocumentSnapshot written with ID: ${documentReference.id}")
                val intent = Intent(this, StartGameActivity::class.java)
                playerPlaying.username = roomUser!!.username
                playerPlaying.profileImageUrl = roomUser!!.profileImageUrl
                updateUID(roomCode, roomUser!!.uid)
                uploadToFirestore(playerPlaying, roomCode)
                intent.putExtra(ROOM_CODE, roomCode)
                intent.putExtra(USER_KEY_SIGNUP, roomUser)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
//                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
            .addOnFailureListener { e ->
                Log.d("Room", "Error adding document")
            }

        val gameRef = db.collection("rooms").document(roomCode)
            .collection("game").document("playersnum")

        val count = hashMapOf("count" to 0)
        gameRef
            .set(count)
            .addOnSuccessListener {
                Log.d("GamePlay", "count = $count")
            }

    }


    private fun tryJoinRoom(enteredRoomCode: String) {
        val db = Firebase.firestore

        val falseDocRef = db.collection("rooms").document(enteredRoomCode)

        userEntry =
            falseDocRef.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                if (documentSnapshot != null && documentSnapshot.exists() && !alreadyJoined) {
                    Log.d("joinRoom", "Current data: ${documentSnapshot.data}")
                    hasGameStarted(enteredRoomCode)
                } else {
                    button_final_join.isEnabled = true
                    Log.d("Check", "Please enter a valid room code")
                    Toast.makeText(this, "Invalid Room Code!!", Toast.LENGTH_SHORT).show()
                }
            }


    }

    private fun joinRoom(roomCode: String) {
        val db = Firebase.firestore
        val documentRef = db.collection("rooms").document(roomCode)
            .collection("players")

        documentRef
            .add(roomUser!!)
            .addOnSuccessListener { documentReference ->
                Log.d("Room", "DocumentSnapshot written with ID: ${documentReference.id}")
                updateJoinedPlayersCount(roomCode)
                playerPlaying.username = roomUser!!.username
                playerPlaying.profileImageUrl = roomUser!!.profileImageUrl
                uploadToFirestore(playerPlaying, enteredRoomCode)
                updateUID(enteredRoomCode, roomUser!!.uid)
                startJoinRoomActivity(roomCode)
            }
            .addOnFailureListener { e ->
                Log.d("Room", "Error adding document")
            }
    }

    private fun updateUID(roomCode: String, uid: String) {
        val db = Firebase.firestore
        val uidRef = db.collection("rooms").document(roomCode)
            .collection("allowedUID").document(uid)

        val hashmap = hashMapOf("uid" to uid)

        uidRef
            .set(hashmap)

    }

    private fun startJoinRoomActivity(roomCode: String) {
        val intent = Intent(this, StartJoinActivity::class.java)
        alreadyJoined = true
        intent.putExtra(ROOM_CODE, roomCode)
        intent.putExtra(USER_KEY_SIGNUP, roomUser)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
//        overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
        removeListener()
    }

    private fun removeListener() {
        userEntry?.remove()
    }


    private fun hasGameStarted(roomCode: String) {
        userEntry?.remove()

        val db = Firebase.firestore
        val gameRef = db.collection("rooms").document(roomCode)

        gameRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    hasGameStarted = document.get("isGameStarted") as Boolean
                    Log.d("checkjoining", "joiningAllowed = $hasGameStarted")
                    entryAllowed(roomCode, hasGameStarted)
                } else {
                    Log.d(TAG, "No such document")
                    hasGameStarted = true
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
                hasGameStarted = true
            }
    }


    private fun uploadToFirestore(currentPlayer: Player, receivedRoomcode: String) {
        val db = Firebase.firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val gameRef = db.collection("rooms").document(receivedRoomcode)
            .collection("leaderBoardPlayers").document(uid)

        gameRef
            .set(currentPlayer)
            .addOnSuccessListener { documentReference ->
                Log.d("GameActivity", "DocumentSnapshot written with ID: $uid")
                updateCount(uid, receivedRoomcode)
            }
            .addOnFailureListener { e ->
                Log.d("GameActivity", "Error adding document")
            }
    }

    private fun updateCount(docId: String, receivedRoomcode: String) {
        val db = Firebase.firestore
        val gameRef = db.collection("rooms").document(receivedRoomcode)
            .collection("game").document("playersnum")

        var playersCount = -10

        gameRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("Update", "DocumentSnapshot data: ${document.data}")
                    playersCount = (document.get("count") as Long).toInt()
                    playersCount += 1
                    updatePlayCount(playersCount, receivedRoomcode)
                    updateIndex(playersCount, docId, receivedRoomcode)
                } else {
                    Log.d("Update", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Update", "get failed with ", exception)
            }

    }

    private fun updatePlayCount(num: Int, receivedRoomcode: String) {
        playerPlaying.indexOfTurn = num
        val db = Firebase.firestore
        val gameRef = db.collection("rooms").document(receivedRoomcode)
            .collection("game").document("playersnum")
        gameRef
            .update("count", num)
            .addOnSuccessListener { Log.d("Update", "DocumentSnapshot successfully updated!") }
            .addOnFailureListener { e -> Log.w("Update", "Error updating document", e) }
    }

    private fun updateIndex(num: Int, docId: String, receivedRoomcode: String) {
        val db = Firebase.firestore
        val playerRef = db.collection("rooms").document(receivedRoomcode)
            .collection("leaderBoardPlayers").document(docId)

        playerRef
            .update("indexOfTurn", num)
    }


    private fun updateJoinedPlayersCount(roomCode: String) {
        val db = Firebase.firestore
        val playersCountRef = db.collection("rooms").document(roomCode)

        var playersCount: Int = 0

        playersCountRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    playersCount = (document.get("playersCount") as Long).toInt()
                    playersCount++
                    playersCountRef
                        .update("playersCount", playersCount)
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "DocumentSnapshot successfully updated!"
                            )
                        }
                        .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

    }


    private fun entryAllowed(roomCode: String, gameStart: Boolean) {
        if (!gameStart) {
            val db = Firebase.firestore
            val playersCountRef = db.collection("rooms").document(roomCode)

            playersCountRef
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d("entryAllowed", "DocumentSnapshot data: ${document.data}")
                        val playersCount = (document.get("playersCount") as Long).toInt()
                        val maxPlayers = (document.get("maxPlayers") as Long).toInt()
                        if (playersCount < maxPlayers) {
                            joinRoom(roomCode)
                        } else {
                            Toast.makeText(this, "Room is already full!", Toast.LENGTH_SHORT).show()
                            button_final_join.isEnabled = true
                        }
                    } else {
                        Log.d("entryAllowed", "No such document")
                    }
                }
        } else {
            Toast.makeText(this, "Game already Started!", Toast.LENGTH_SHORT).show()
            button_final_join.isEnabled = true
        }


    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_sign_out -> {
                AlertDialog.Builder(this)
                    .setTitle("Confirm Sign Out")
                    .setMessage("Are you sure you want to Sign Out?")
                    .setPositiveButton(
                        "YES"
                    ) { dialog, which ->
                        Log.d("MainActivity", "Sending atomic bombs to Jupiter")
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, SignupActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    .setNegativeButton(
                        "Cancel"
                    ) { dialog, which -> Log.d("MainActivity", "Aborting mission...") }
                    .show()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

}




