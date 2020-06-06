package com.prayutsu.sckribbel.play

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.GuessText
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.register.SignupActivity
import com.prayutsu.sckribbel.room.RoomActivity.Companion.ROOM_CODE
import com.prayutsu.sckribbel.room.RoomActivity.Companion.playerPlaying
import com.prayutsu.sckribbel.room.GameResultsActivity
import com.prayutsu.sckribbel.room.StartJoinActivity
import com.prayutsu.sckribbel.room.WordsArray
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.min

class GameActivity : AppCompatActivity(), View.OnClickListener {

    private var _doubleBackToExitPressedOnce = false
    private var myPaintView: PaintView? = null
    private var currentTurnInGame = 0
    private var startTimer = false
    private val db = Firebase.firestore
    private val fbdb = FirebaseDatabase.getInstance()
    var roomCode = ""
    var currentUser: User? = null
    var currentPlayer: Player? = null
    var maxPlayersNum = 0
    var numberOfRounds = 0
    var currentDrawerUsername = ""
    var chosenWordByDrawer = ""
    lateinit var timerForChoosingWord: CountDownTimer
    var timerRunning = true
    var timerFinished = false
    var finishTimer = false

    var count = 0
    var roundNo = 1

    val MAX_POINTS = 500

    var timeRemaining = 0

    lateinit var currentDrawerTimer: CountDownTimer
    lateinit var otherPlayerTimer: CountDownTimer

    private var rankListener: ListenerRegistration? = null

    var nearlyEqual = false
    private val guessAdapter = GroupAdapter<GroupieViewHolder>()
    private val leaderboardAdapter = GroupAdapter<GroupieViewHolder>()


    companion object {
        val TAG = "Game Activity"
        val playersList = mutableListOf<Player>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roomCode = intent.getStringExtra(StartJoinActivity.JOIN_USER_KEY) ?: return
        currentUser = intent.getParcelableExtra(SignupActivity.USER_KEY_SIGNUP)
        myPaintView = drawing_board
        myPaintView?.roomCodePaintView = roomCode

        myPaintView?.ref = fbdb.getReference("games/$roomCode/drawing")

        currentPlayer = playerPlaying
        myPaintView?.invalidate()
//        myPaintView?.addDatabaseListeners()

        chat_log_recyclerview.adapter = guessAdapter
        val linearLayoutManager = LinearLayoutManager(this)
        chat_log_recyclerview.layoutManager = linearLayoutManager

        leaderboard_recyclerview.adapter = leaderboardAdapter

        chat_log_recyclerview.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (bottom < oldBottom) {
                    if (guessAdapter.itemCount - 1 > 0)
                        linearLayoutManager.smoothScrollToPosition(
                            chat_log_recyclerview,
                            null,
                            guessAdapter.itemCount - 1
                        )
                }
            }
        })

        guess_button.setOnClickListener {
            if (guess_editText.text != null)
                performGuess()
            else
                return@setOnClickListener
        }

        color_pallette.setOnClickListener {
            colorPaletteDisplay();
        }

        eraser.setOnClickListener {
            myPaintView?.eraser()
        }

        clear_button.setOnClickListener {
            myPaintView?.clearDrawingForAll()
        }
        detectCurrentDrawerUsername()
        findMaxPlayers()
        obtainCurrentTurn()
        detectTimerStart()
        listenChosenWord()
        listenGuesses()
        alreadyGuessListener()
        msgListener()
        checkRank()
        roundListener()
        leaderBoardListener()
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

    override fun onClick(view: View) {
        if (currentPlayer?.currentDrawer!!) {
            when (view.id) {
                R.id.first_word_textview -> {
                    activateBoard()
                    clear_button.performClick()
                    timerForChoosingWord.cancel()
                    uploadChosenWord(first_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.INVISIBLE
                    first_word_textview.visibility = View.INVISIBLE
                    second_word_textview.visibility = View.INVISIBLE
                    third_word_textview.visibility = View.INVISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()
                }
                R.id.second_word_textview -> {
                    activateBoard()
                    clear_button.performClick()
                    timerForChoosingWord.cancel()
                    uploadChosenWord(second_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.INVISIBLE
                    first_word_textview.visibility = View.INVISIBLE
                    second_word_textview.visibility = View.INVISIBLE
                    third_word_textview.visibility = View.INVISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()

                }
                R.id.third_word_textview -> {
                    activateBoard()
                    clear_button.performClick()
                    timerForChoosingWord.cancel()
                    uploadChosenWord(third_word_textview.text.toString())
                    third_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    first_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.INVISIBLE
                    first_word_textview.visibility = View.INVISIBLE
                    second_word_textview.visibility = View.INVISIBLE
                    third_word_textview.visibility = View.INVISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()

                }
            }
        }
    }


    private fun uploadChosenWord(chosenWord: String) {
        val wordRef = db.collection("rooms").document(roomCode)
            .collection("game").document("correctWord")

        val chosenWordMap = hashMapOf("chosenWord" to chosenWord)

        wordRef
            .set(chosenWordMap)
    }


    private fun showViewToPlayer() {
        if (currentPlayer?.currentDrawer!!) {
            setWords()
            wordChoosingTimer()
            choose_a_word_textview.visibility = View.VISIBLE
            first_word_textview.visibility = View.VISIBLE
            second_word_textview.visibility = View.VISIBLE
            third_word_textview.visibility = View.VISIBLE
            first_word_textview.setOnClickListener(this)
            second_word_textview.setOnClickListener(this)
            third_word_textview.setOnClickListener(this)

            first_word_textview.isEnabled = true
            second_word_textview.isEnabled = true
            third_word_textview.isEnabled = true
            someone_choosing_a_word_textview.visibility = View.INVISIBLE

        } else {
            eraser.visibility = View.INVISIBLE
            color_pallette.visibility = View.INVISIBLE
            clear_button.visibility = View.INVISIBLE

            choose_a_word_textview.visibility = View.INVISIBLE
            first_word_textview.visibility = View.INVISIBLE
            second_word_textview.visibility = View.INVISIBLE
            third_word_textview.visibility = View.INVISIBLE
            myPaintView?.allowDraw = false
            someone_choosing_a_word_textview.visibility = View.INVISIBLE

        }
    }

    private fun wordChoosingTimer() {
        timerForChoosingWord = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerRunning = true
            }

            override fun onFinish() {
                timer_textview.text = "Word Chosen Automatically!"
                timerRunning = false
                autoSelectWord()
            }
        }
        timerForChoosingWord.start()
    }


    private fun checkCurrentDrawer() {
        currentPlayer?.currentDrawer = currentTurnInGame == currentPlayer?.indexOfTurn
        if (currentPlayer?.currentDrawer!!) {
            currentPlayer?.username?.let { updateDrawerName(it) }
            count = 0
            finishTimer = false
//            checkRank()
        }
        detectCurrentDrawerUsername()
        showViewToPlayer()


    }


    private fun startTimerCurrentDrawer() {
        currentDrawerTimer =
            object : CountDownTimer(90000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timer_textview.text = (millisUntilFinished / 1000).toString()
                    timeRemaining = millisUntilFinished.toInt()
                    timerFinished = false
                }

                override fun onFinish() {
//                timer_textview.text = "done!"
                    myPaintView?.clearDrawingForAll()
                    timerFinished = true
                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")
                    val turnRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("turn")

                    currentPlayer?.currentDrawer = false
                    currentTurnInGame += 1
                    if (currentTurnInGame > maxPlayersNum) {
                        currentTurnInGame = 1
                        roundNo += 1
                        updateRound()
                    }
                    timerRef
                        .update("startTimer", false)
                        .addOnSuccessListener {
                            Log.d(TAG, "timer updated: false")
                        }
                    turnRef
                        .update("currentTurn", currentTurnInGame)
                        .addOnSuccessListener {
                            Log.d(TAG, "turn updated: $currentTurnInGame")
                        }
                }
            }
        currentDrawerTimer.start()

    }

    private fun startTimer() {

        otherPlayerTimer =
            object : CountDownTimer(90000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerFinished = false
                    timer_textview.text = (millisUntilFinished / 1000).toString()
                    timeRemaining = (millisUntilFinished / 1000).toInt()
                }

                override fun onFinish() {
//                timer_textview.text = "done!"
                    timerFinished = true
                }
            }
        otherPlayerTimer.start()
    }

    private fun updateDrawerName(currentDrawerName: String) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("currentDrawer")

        val drawerMap = hashMapOf("currentDrawerName" to currentDrawerName)

        drawerRef
            .set(drawerMap)
    }

    private fun detectTimerStart() {
        val timerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("timer")

        timerRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Timer start : ${snapshot.data}")
                    startTimer = snapshot.get("startTimer") as Boolean

                    if (startTimer) {
                        startTimer()
                        someone_choosing_a_word_textview.visibility = View.INVISIBLE
                    }

                } else {
                    Log.d(TAG, "Current data in turn: null")
                }
            }
    }


    private fun obtainCurrentTurn() {
        val turnRef = db.collection("rooms").document(roomCode)
            .collection("game").document("turn")

        turnRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data in turn to initiate view: ${snapshot.data}")
                    currentTurnInGame = (snapshot.get("currentTurn") as Long).toInt()
                    currentPlayer?.hasAlreadyGuessed = false
                    writeHasAlreadyGuessed()
                    checkCurrentDrawer()
                } else {
                    Log.d(TAG, "Current data in turn: null")
                }
            }
    }

    private fun writeHasAlreadyGuessed() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasGuessRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        hasGuessRef
            .update("hasAlreadyGuessed", false)
    }

    private fun detectCurrentDrawerUsername() {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("game").document("currentDrawer")

        drawerRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    currentDrawerUsername = snapshot.getString("currentDrawerName").toString()
                    someone_choosing_a_word_textview.text =
                        "$currentDrawerUsername is choosing a word!!"

                    if (!currentPlayer?.currentDrawer!!)
                        someone_choosing_a_word_textview.visibility = View.VISIBLE

                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun listenChosenWord() {
        val wordRef = db.collection("rooms").document(roomCode)
            .collection("game").document("correctWord")

        wordRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    chosenWordByDrawer = snapshot.getString("chosenWord").toString()
                    Log.d(TAG, "Current word: $chosenWordByDrawer")

                } else {
                    Log.d(TAG, "Current data in word : null")
                }
            }
    }

    private fun findMaxPlayers() {
        val maxPlayersRef = db.collection("rooms").document(roomCode)

        maxPlayersRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    maxPlayersNum = (document.get("maxPlayers") as Long).toInt()
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun msgListener() {
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")
        msgRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (dc in snapshot.documentChanges) {
                        Log.d(TAG, "Current data: $snapshot")
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val username = dc.document.getString("username").toString()
                                val award = (dc.document.get("reward") as Long).toInt().toString()
                                Toast.makeText(this, "$username +$award", Toast.LENGTH_SHORT).show()
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(TAG, "Modified city: ${dc.document.data}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(TAG, "Removed city: ${dc.document.data}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun listenGuesses() {
        val guessRef = db.collection("rooms").document(roomCode)
            .collection("guesses")

        var guessListened: String

        guessRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (dc in snapshot.documentChanges) {
                        Log.d(TAG, "Current data: $snapshot")
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                Log.d(TAG, "New city: ${dc.document.data}")
                                guessListened = dc.document.getString("guess").toString()
                                val username = dc.document.getString("username").toString()
                                val profileImageUrl =
                                    dc.document.getString("profileImageUrl").toString()
                                val timeStamp = dc.document.get("timeStamp") as Long
                                val hasAlreadyGuessed = dc.document.getBoolean("hasAlreadyGuessed")
                                val hasGuess = hasAlreadyGuessed ?: return@addSnapshotListener
                                val uid =
                                    dc.document.getString("playerUid") ?: return@addSnapshotListener
                                val timeLeft = (dc.document.get("timeRemaining") as Long).toInt()
                                checkGuess(
                                    guessListened,
                                    username,
                                    profileImageUrl,
                                    hasGuess,
                                    uid,
                                    timeLeft,
                                    timeStamp
                                )
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(TAG, "Modified city: ${dc.document.data}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(TAG, "Removed city: ${dc.document.data}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun awardPoints(uid: String, reward: Int, username: String) {
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)

        playerRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    var prevPoints = (document.get("points") as Long).toInt()
                    finalAwardPoints(prevPoints, reward, uid, username)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

    }

    private fun finalAwardPoints(prevPoints: Int, reward: Int, uid: String, username: String) {
        val newPoints = prevPoints + reward
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        playerRef
            .update("points", newPoints)

        val msgMap =
            hashMapOf("username" to username, "reward" to reward)
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")

        msgRef
            .add(msgMap)


        ////delete correctguesstimestamp only if it is read by all users....

//        val stampRef = db.collection("rooms").document(roomCode)
//            .collection("correctGuessTimeStamp").document(uid)
//
//        stampRef
//            .delete()

        if (finishTimer) {
            currentDrawerTimer.cancel()
            currentDrawerTimer.onFinish()
        }
    }

    private fun awardPointsToDrawer(drawerReward: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        val username = currentDrawerUsername
        drawerRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val prevPoints = (document.get("points") as Long).toInt()
                    finalAwardPointsToDrawer(prevPoints, drawerReward, uid, username)
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

    }

    private fun finalAwardPointsToDrawer(
        prevPoints: Int,
        drawerReward: Int,
        uid: String,
        username: String
    ) {
        val newPoints = prevPoints + drawerReward
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)
        playerRef
            .update("points", newPoints)

        val msgMap =
            hashMapOf("username" to username, "reward" to drawerReward)
        val msgRef = db.collection("rooms").document(roomCode)
            .collection("message")

        msgRef
            .add(msgMap)


    }

    private fun checkRank() {
        val timeStampRef = db.collection("rooms").document(roomCode)
            .collection("correctGuessTimeStamp")

        rankListener =
            timeStampRef
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (dc in snapshot.documentChanges) {
                            Log.d(TAG, "Current data: $snapshot")
                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    ++count
                                    Log.d(TAG, "New city: ${dc.document.data}")
                                    val uid = dc.document.getString("uid").toString()
                                    val username = dc.document.getString("username").toString()
                                    val timeLeft = (dc.document.get("timeLeft") as Long).toInt()

                                    val reward =
                                        MAX_POINTS - (count - 1) * (300 / maxPlayersNum) - (90 - timeLeft) * 2

                                    val drawerReward = (reward / maxPlayersNum)

                                    if (currentPlayer?.currentDrawer!!) {
                                        awardPoints(uid, reward, username)
                                        awardPointsToDrawer(drawerReward)
                                    }
                                    if (count == maxPlayersNum - 1) {
                                        finishTimer = true

                                    }
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    Log.d(TAG, "Modified city: ${dc.document.data}")
                                    ++count
                                    Log.d(TAG, "New city: ${dc.document.data}")
                                    val uid = dc.document.getString("uid").toString()
                                    val username = dc.document.getString("username").toString()
                                    val timeLeft = (dc.document.get("timeLeft") as Long).toInt()

                                    val reward =
                                        MAX_POINTS - (count - 1) * (300 / maxPlayersNum) - (90 - timeLeft) * 2

                                    val drawerReward = (reward / maxPlayersNum)

                                    if (currentPlayer?.currentDrawer!!) {
                                        awardPoints(uid, reward, username)
                                        awardPointsToDrawer(drawerReward)
                                    }
                                    if (count == maxPlayersNum - 1) {
                                        finishTimer = true
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    Log.d(TAG, "Removed city: ${dc.document.data}")
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Current data: null")
                    }
                }

    }

    private fun checkNearlyEqual(l: Int, checkGuess: String, correctWord: String) {
        var count = 0
        for (i in 0 until l) {
            if (checkGuess[i] == correctWord[i]) {
                count++
            }
        }
        if (l - count == 0 || l - count == 1)
            nearlyEqual = true
    }

    private fun checkGuess(
        guessWord: String, username: String, profileImageUrl: String, hasAlreadyGuessed: Boolean,
        playerUid: String, timeRemain: Int, timestamp: Long
    ) {

        val checkGuess = guessWord.toLowerCase()
        val correctWord = chosenWordByDrawer.toLowerCase()
        val l1 = checkGuess.length
        val l2 = correctWord.length
        if (l1 != l2) {
            val l = min(l1, l2)
            checkNearlyEqual(l, checkGuess, correctWord)
        }


        var guessMessage = GuessText("")
        guessMessage.username = username
        guessMessage.profileImageUrl = profileImageUrl


        if (!timerFinished) {
            if (checkGuess == correctWord) {
                if (username == currentDrawerUsername) {
                    guessMessage.guessText = "$username is penalized with -100 points!"
                    guessAdapter.add(guessMessage)
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                    if (currentPlayer?.username == username)
                        awardPoints(playerUid, -100, username)

                } else {
                    if (!hasAlreadyGuessed) {
                        guessMessage.guessText = "$username guessed the correct word!"
                        guessAdapter.add(guessMessage)
                        chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                        //award points
                        if (currentPlayer?.username == username) {
                            val stampMap =
                                hashMapOf(
                                    "uid" to playerUid,
                                    "username" to username,
                                    "timeLeft" to timeRemain
                                )

                            val timeStampRef = db.collection("rooms").document(roomCode)
                                .collection("correctGuessTimeStamp").document(playerUid)
                            timeStampRef
                                .set(stampMap)

                            val playerRef = db.collection("rooms").document(roomCode)
                                .collection("leaderBoardPlayers").document(playerUid)
                            playerRef
                                .update("hasAlreadyGuessed", true)
                        }
                    } else {
                        guessMessage.guessText = "$username already guessed the correct word!"
                        guessAdapter.add(guessMessage)
                        chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                        //Don't award points
                    }
                }
            } else if (nearlyEqual && !hasAlreadyGuessed) {
                if (username == currentDrawerUsername) {
                    guessMessage.guessText = "$username is penalized with -100 points!"
                    guessAdapter.add(guessMessage)
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                } else {
                    guessMessage.guessText = "$username's guess is nearly correct!"
                    guessAdapter.add(guessMessage)
                    nearlyEqual = false
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                }
            } else {
                guessMessage.guessText = guessWord
                guessAdapter.add(guessMessage)
                chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
            }
        } else {
            guessMessage.guessText = guessWord
            guessAdapter.add(guessMessage)
            chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
        }

    }


    private fun alreadyGuessListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasGuessRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)

        hasGuessRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    currentPlayer?.hasAlreadyGuessed =
                        snapshot.getBoolean("hasAlreadyGuessed") as Boolean
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }

    }

    private fun performGuess() {

        val guessRef = db.collection("rooms").document(roomCode)
            .collection("guesses")

        val guessMsg = guess_editText.text.toString()

        val guessMap =
            hashMapOf(
                "guess" to guessMsg,
                "timeStamp" to System.currentTimeMillis(),
                "playerUid" to FirebaseAuth.getInstance().currentUser?.uid,
                "hasAlreadyGuessed" to currentPlayer?.hasAlreadyGuessed,
                "username" to currentPlayer?.username,
                "profileImageUrl" to currentPlayer?.profileImageUrl,
                "timeRemaining" to timeRemaining
            )

        guessRef
            .add(guessMap)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

        guess_editText.text.clear()

    }

    private fun updateRound() {
        val roundRef = db.collection("rooms").document(roomCode)
            .collection("game").document("round")
        roundRef
            .update("currentRound", roundNo)
    }

    private fun roundListener() {
        val roundRef = db.collection("rooms").document(roomCode)
            .collection("game").document("round")
        roundRef
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    roundNo = (snapshot.get("currentRound") as Long).toInt()
                    round_textview.text = "Round $roundNo of 3"
                    if (roundNo == 4) {
                        //declare results and game over!!

                        val intent = Intent(this, GameResultsActivity::class.java)
                        intent.putExtra(ROOM_CODE, roomCode)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)

                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun activateBoard() {
        drawing_board.performClick()
        drawing_board.performClick()

        val segment = DrawSegment()
        val scale = 1.0f
        segment.addColor(Color.BLACK)

        segment.addPoint(243, 0)
        segment.addPoint(252, 0)
        segment.addPoint(260, 0)
        segment.addPoint(266, 0)
        segment.addPoint(272, 0)
        segment.addPoint(278, 0)

        myPaintView?.invalidate()
        myPaintView?.canvas!!.drawPath(
            PaintView.getPathForPoints(segment.points, scale.toDouble()),
            myPaintView?.paint ?: return
        )
        val drawId = UUID.randomUUID().toString().substring(0, 15)


        val db = FirebaseDatabase.getInstance()

        val keyRef = db.getReference("games/$roomCode/drawing/$drawId")
        Log.d("MainActivity", "Saving segment to firebase")
        keyRef
            .setValue(segment)
    }

    private fun setWords() {
        val random = Random()
        val first = random.nextInt(91) + 1
        val second = random.nextInt(91) + 1
        val third = random.nextInt(91) + 1
        first_word_textview.text = WordsArray.words[first]
        second_word_textview.text = WordsArray.words[second]
        third_word_textview.text = WordsArray.words[third]
    }

    private fun colorPaletteDisplay() {
        val mColor = Color.RED

        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose color for the pencil")
            .initialColor(mColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
            .density(8)
            .setOnColorSelectedListener { selectedColor ->
                Toast.makeText(this, "selected color: $selectedColor", Toast.LENGTH_SHORT)
            }
            .setPositiveButton(
                "pick"
            ) { dialog, selectedColor, allColors -> changeColor(selectedColor) }
            .setNegativeButton(
                "cancel"
            ) { dialog, which -> }
            .build()
            .show()
    }

    private fun changeColor(selectedColor: Int) {
        myPaintView?.changeStrokeColor(selectedColor)
    }

    private fun autoSelectWord() {
        val random = Random()
        when (random.nextInt(2)) {
            0 -> {
                first_word_textview.performClick()
            }
            1 -> {
                second_word_textview.performClick()
            }
            else -> {
                third_word_textview.performClick()
            }
        }
    }

    private fun leaderBoardListener() {
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers")
        var player: Player
        playerRef
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "New city: ${dc.document.data}")
                            val username = dc.document.getString("username").toString()
                            player = dc.document.toObject(Player::class.java)
                            playersList.removeAll { it.username == username }
                            playersList.add(player)
                            updateRecyclerView()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            Log.d(TAG, "Modified city: ${dc.document.data}")
                            val username = dc.document.getString("username").toString()
//                            val pfu = dc.document.getString("profileImageUrl").toString()
                            player = dc.document.toObject(Player::class.java)
                            playersList.removeAll { it.username == username }
                            playersList.add(player)
                            updateRecyclerView()
                            if (finishTimer) {
                                otherPlayerTimer.cancel()
                                otherPlayerTimer.onFinish()
                            }
                        }
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed city: ${dc.document.data}"
                        )
                    }
                }
            }
    }

    private fun updateRecyclerView() {
        playersList.sort()
        if (leaderboardAdapter.itemCount > 0) {
            leaderboardAdapter.clear()
        }
        for (player in playersList) {
            leaderboardAdapter.add(LeaderboardItem(player))
        }
    }


}


