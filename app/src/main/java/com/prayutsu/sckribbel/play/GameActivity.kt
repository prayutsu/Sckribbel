package com.prayutsu.sckribbel.play

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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
import com.prayutsu.sckribbel.room.GameResultsActivity
import com.prayutsu.sckribbel.room.RoomActivity.Companion.ROOM_CODE
import com.prayutsu.sckribbel.room.RoomActivity.Companion.playerPlaying
import com.prayutsu.sckribbel.room.StartJoinActivity
import com.prayutsu.sckribbel.room.WordsArray
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_toast.*
import java.util.*
import kotlin.math.min


class GameActivity : AppCompatActivity(), View.OnClickListener {

    private var _doubleBackToExitPressedOnce = false
    private var currentTurnInGame = 0
    private var startTimer = false
    private val db = Firebase.firestore
    private val fbdb = FirebaseDatabase.getInstance()
    private val myUid = FirebaseAuth.getInstance().uid
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
    var flagOfWritingFinishTimer = false
    var text = ""

    var count = 0
    var roundNo = 1

    val MAX_POINTS = 400

    var timeRemaining = 0

    lateinit var currentDrawerTimer: CountDownTimer
    var otherPlayerTimer: CountDownTimer? = null

    private var rankListener: ListenerRegistration? = null
    private var drawRewardListen: ListenerRegistration? = null

    var nearlyEqual = false
    private val guessAdapter = GroupAdapter<GroupieViewHolder>()
    private val leaderboardAdapter = GroupAdapter<GroupieViewHolder>()
    lateinit var soundPool: SoundPool
    var correctGuessSound: Int? = null
    var penaltySound: Int? = null


    lateinit var fade_in: Animation
    lateinit var fade_out: Animation
    lateinit var top_anim: Animation
    lateinit var bottom_anim: Animation
    lateinit var ltr: Animation
    lateinit var rtl: Animation
    lateinit var rtl_remove: Animation

    var colorArray = listOf<Int>(
        Color.parseColor("#fe548b"),
        Color.parseColor("#feaa46"),
        Color.parseColor("#43bcfe"),
        Color.parseColor("#3fdec3"),
        Color.parseColor("#a97cf4"),
        Color.parseColor("#ffeb3b")
    )

    companion object {
        val TAG = "Game Activity"
        val playersList = mutableListOf<Player>()
        var myPaintView: PaintView? = null
        var seekProgress = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        fade_in = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow)
        fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out_slow)
        top_anim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottom_anim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        ltr = AnimationUtils.loadAnimation(this, R.anim.left_animation)
        rtl = AnimationUtils.loadAnimation(this, R.anim.right_animation)
        rtl_remove = AnimationUtils.loadAnimation(this, R.anim.right_to_left_invisible)

        drawing_board.animation = top_anim
        chat_log_recyclerview.animation = bottom_anim
        leaderboard_recyclerview.animation = bottom_anim
        clock_imageview.animation = ltr
        logo_game.animation = rtl
        roomCode = intent.getStringExtra(StartJoinActivity.JOIN_USER_KEY) ?: return
        currentUser = intent.getParcelableExtra(SignupActivity.USER_KEY_SIGNUP)
        myPaintView = drawing_board
        myPaintView?.roomCodePaintView = roomCode
        val task = myPaintView?.TouchEvent()
        task?.execute("abhay")
        myPaintView?.setOnTouchListener(task)

        myPaintView?.ref = fbdb.getReference("games/$roomCode/drawing")

        currentPlayer = playerPlaying
        myPaintView?.invalidate()
        myPaintView?.addDatabaseListeners()

        soundPool = SoundPool(2, AudioManager.STREAM_MUSIC, 0)
        correctGuessSound = soundPool.load(applicationContext, R.raw.correct_guess, 1)
        penaltySound = soundPool.load(applicationContext, R.raw.cheating, 1)

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
            if (guess_editText.text.toString() != "")
                performGuess()
            else {
                return@setOnClickListener
            }
        }
        color_pallette.setOnClickListener(this)
        eraser.setOnClickListener(this)
        clear_button.setOnClickListener(this)
        stroke_imageButton.setOnClickListener(this)
        pencil.setOnClickListener(this)

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
        listenCorrectGuessNum()
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

    override fun onClick(view: View) {
        if (currentPlayer?.currentDrawer!!) {
            when (view.id) {
                R.id.first_word_textview -> {
                    eraser.visibility = View.VISIBLE
                    pencil.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr
                    choose_a_word_textview.animation = top_anim
                    activateBoard()
                    timerForChoosingWord.cancel()
                    uploadChosenWord(first_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE

                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)
                    count = 0
                    startTimerCurrentDrawer()
                    clear_button.performClick()

                }
                R.id.second_word_textview -> {
                    activateBoard()
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr

                    timerForChoosingWord.cancel()
                    uploadChosenWord(second_word_textview.text.toString())
                    first_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    third_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE
                    eraser.visibility = View.VISIBLE
                    pencil.visibility = View.VISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()
                    clear_button.performClick()
                    count = 0
                }
                R.id.third_word_textview -> {
                    activateBoard()
                    first_word_textview.animation = fade_out
                    second_word_textview.animation = fade_out
                    third_word_textview.animation = fade_out
                    eraser.animation = ltr
                    color_pallette.animation = ltr
                    clear_button.animation = ltr
                    stroke_imageButton.animation = ltr
                    pencil.animation = ltr

                    timerForChoosingWord.cancel()
                    uploadChosenWord(third_word_textview.text.toString())
                    third_word_textview.isEnabled = false
                    second_word_textview.isEnabled = false
                    first_word_textview.isEnabled = false
                    choose_a_word_textview.visibility = View.GONE
//                    first_word_textview.visibility = View.INVISIBLE
//                    second_word_textview.visibility = View.INVISIBLE
//                    third_word_textview.visibility = View.INVISIBLE
                    pencil.visibility = View.VISIBLE
                    eraser.visibility = View.VISIBLE
                    color_pallette.visibility = View.VISIBLE
                    clear_button.visibility = View.VISIBLE
                    stroke_imageButton.visibility = View.VISIBLE
                    myPaintView?.allowDraw = true

                    val timerRef = db.collection("rooms").document(roomCode)
                        .collection("game").document("timer")

                    timerRef
                        .update("startTimer", true)

                    startTimerCurrentDrawer()
                    clear_button.performClick()

                    count = 0
                }

                R.id.eraser -> {
                    myPaintView?.eraser()
                }
                R.id.clear_button -> {
                    myPaintView?.clearDrawingForAll()
                }
                R.id.color_pallette -> {
                    colorPaletteDisplay()
                }
                R.id.stroke_imageButton -> {
                    val cdd = DialogStroke(this)
                    cdd.show()
                }
                R.id.pencil -> {
                    myPaintView?.paint?.color = Color.BLACK
                    myPaintView?.paint?.strokeWidth = 15f
                }
            }
        }
    }


    private fun showViewToPlayer() {
        count = 0
        if (currentPlayer?.currentDrawer!!) {
            setWords()
            wordChoosingTimer()
            first_word_textview.animation = fade_in
            second_word_textview.animation = fade_in
            third_word_textview.animation = fade_in
            choose_a_word_textview.animation = fade_in
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

        } else {
            eraser.animation = rtl_remove
            color_pallette.animation = rtl_remove
            clear_button.animation = rtl_remove
            stroke_imageButton.animation = rtl_remove
            pencil.animation = rtl_remove
            pencil.visibility = View.GONE
            eraser.visibility = View.GONE
            color_pallette.visibility = View.GONE
            clear_button.visibility = View.GONE
            stroke_imageButton.visibility = View.GONE
            choose_a_word_textview.visibility = View.GONE
            first_word_textview.visibility = View.GONE
            second_word_textview.visibility = View.GONE
            third_word_textview.visibility = View.GONE
            myPaintView?.allowDraw = false
        }
    }


    private fun checkCurrentDrawer() {
        currentPlayer?.currentDrawer = currentTurnInGame == currentPlayer?.indexOfTurn
        if (currentPlayer?.currentDrawer!!) {
            currentPlayer?.username?.let { updateDrawerName(it) }
            count = 0
            finishTimer = false
            timerFinished = false
            flagOfWritingFinishTimer = false
            show_correct_word_textview.visibility = View.GONE
            resetCorrectGuessNum()
            showViewToPlayer()
        } else {
            showViewToPlayer()
        }
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
                    if (myUid != null) {
                        drawerRewardListener(currentDrawerUsername, myUid)
                    }
                    timer_textview.text = ""
                    myPaintView?.clearDrawingForAll()
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
                    chosen_word_textview.text = ""
                }
            }
        currentDrawerTimer.start()

    }

    private fun startTimer() {

        otherPlayerTimer =
            object : CountDownTimer(90000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    show_correct_word_textview.visibility = View.GONE
                    timerFinished = false
                    timer_textview.text = (millisUntilFinished / 1000).toString()
                    timeRemaining = (millisUntilFinished / 1000).toInt()

                    val l = chosenWordByDrawer.length
                    val charAt0 = chosenWordByDrawer[0]
                    val charAtlby2 = chosenWordByDrawer[l / 2]
                    if (timeRemaining == 30 && !currentPlayer?.currentDrawer!!) {
                        text = charAt0 + text.substring(1)
                        chosen_word_textview.text = text
                    }
                    if (timeRemaining == 15 && !currentPlayer?.currentDrawer!!) {
                        text = text.substring(0, (l / 2) * 2) + charAtlby2 +
                                text.substring((l / 2) * 2 + 1)
                        chosen_word_textview.text = text
                    }

                }
                override fun onFinish() {
                    if (!currentPlayer?.currentDrawer!!) {
                        show_correct_word_textview.animation = bottom_anim
                        show_correct_word_textview.visibility = View.VISIBLE
                        show_correct_word_textview.text =
                            "The correct word was \" $chosenWordByDrawer\"!"
                    }
                    timer_textview.text = ""
                    timerFinished = true
                    chosen_word_textview.text = ""

                }
            }
        otherPlayerTimer?.start()
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

                    if (startTimer && !currentPlayer?.currentDrawer!!) {
                        count = 0
                        startTimer()
                        timerFinished = false
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
                    if (otherPlayerTimer != null) {
                        otherPlayerTimer?.cancel()
                        otherPlayerTimer?.onFinish()
                    }
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
                                val award = (dc.document.get("reward") as Long).toInt()
                                val layout: View = layoutInflater.inflate(
                                    R.layout.custom_toast,
                                    custom_toast_layout
                                )
                                val tv =
                                    layout.findViewById(R.id.points_awarded_textview) as TextView
//
                                Log.d("Toast", "Executing")
                                if (award > 0) {
                                    tv.text = "$username  + $award points"
                                    val toast = Toast(applicationContext)
                                    toast.duration = Toast.LENGTH_SHORT
                                    toast.view = layout
                                    toast.setGravity(Gravity.TOP, 0, 130);
                                    toast.show()
                                } else if (award < 0) {
                                    tv.text = "$username  - ${-award} points"
                                    val toast = Toast(applicationContext)
                                    toast.duration = Toast.LENGTH_SHORT
                                    toast.view = layout
                                    toast.setGravity(Gravity.TOP, 0, 130);
                                    toast.show()
                                }
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
                                Log.d(TAG, "New guess: ${dc.document.data}")
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

    private fun awardPoints(uid: String, reward: Int, username: String, drawerReward: Int) {
        val playerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)


        playerRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val prevPoints = (document.get("points") as Long).toInt()
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
    }

    private fun writeDrawerReward(drawerReward: Int) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("drawerReward")

        val hashmap = hashMapOf("drawReward" to drawerReward)

        drawerRef
            .add(hashmap)
    }

    private fun readDrawerPoints(drawerReward: Int, username: String, uid: String) {
        val drawerRef = db.collection("rooms").document(roomCode)
            .collection("leaderBoardPlayers").document(uid)

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

    private fun drawerRewardListener(username: String, uid: String) {
        val ref = db.collection("rooms").document(roomCode)
            .collection("drawerReward")

        var points = 0

        ref
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    points += (document.get("drawReward") as Long).toInt()
                }
                readDrawerPoints(points, username, uid)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        ref
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")

                    val docRef = db.collection("rooms").document(roomCode)
                        .collection("drawerReward").document(document.id)
                    docRef
                        .delete()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

    }

    private fun checkRank() {
        val timeStampRef = db.collection("rooms").document(roomCode)
            .collection("correctGuessTimeStamp")

        rankListener =
            timeStampRef
                .whereEqualTo("uid", myUid)
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    for (doc in value!!) {
                        val uid = doc.getString("uid").toString()
                        val username = doc.getString("username").toString()
                        val timeLeft = (doc.get("timeLeft") as Long).toInt()
                        ++count
                        val reward =
                            MAX_POINTS - (count - 1) * (200 / maxPlayersNum) - (90 - timeLeft) * 2

                        val drawerReward = (reward / maxPlayersNum)

                        if (myUid == uid) {
                            awardPoints(uid, reward, username, drawerReward)
                            writeDrawerReward(drawerReward)

                            val ref = db.collection("rooms").document(roomCode)
                                .collection("correctGuess")

                            val correctMap = hashMapOf("correctGuess" to true)
                            ref
                                .add(correctMap)
                        }

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
        if (l - count == 0 || l - count == 1 || l - count == 2)
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
        val l: Int
        if (l1 != l2) {
            l = min(l1, l2)
        } else {
            l = l1
        }
        checkNearlyEqual(l, checkGuess, correctWord)

        val guessMessage = GuessText("")
        guessMessage.username = username
        guessMessage.profileImageUrl = profileImageUrl
        val random = Random()
        val index = random.nextInt(21) + 1
//        guessMessage.colorPlayer = colorArrayPlayers[index]

        if (!timerFinished) {
            if (checkGuess == correctWord) {
                if (username == currentDrawerUsername) {
                    guessMessage.textColor = Color.parseColor("#c62828")
                    guessMessage.guessText = "$username is penalized with -100 points!"
                    guessAdapter.add(guessMessage)
                    nearlyEqual = false
                    vibrateExecute().execute("abhay")
//                    vibrate()
                    soundPool.play(penaltySound!!, 1.0F, 1.0F, 0, 0, 1.0F);

                    Log.d("GuessingCorrect", "username == currentDrawerUsername getting executed")

                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                    val drawerReward = 0

                    if (currentPlayer?.username == username) {
                        awardPoints(playerUid, -100, username, drawerReward)
                        Log.d(
                            "GuessingCorrect",
                            "currentPlayer?.username == username getting executed"
                        )
                    }

                } else {
                    if (!hasAlreadyGuessed) {
                        guessMessage.guessText = "$username guessed the correct word!"
                        guessMessage.textColor = Color.parseColor("#4caf50")
                        guessAdapter.add(guessMessage)
                        vibrateExecute().execute("garg")
//                        vibrate()
                        Log.d("GuessingCorrect", "!hasAlreadyGuessed getting executed")
                        soundPool.play(correctGuessSound!!, 1.0F, 1.0F, 0, 0, 1.0F);

                        chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)

                        if (currentPlayer?.username == username) {
                            val stampMap =
                                hashMapOf(
                                    "uid" to playerUid,
                                    "username" to username,
                                    "timeLeft" to timeRemain,
                                    "timeStamp" to timestamp
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
                        Log.d("GuessingCorrect", "!hasAlreadyGuessed  else getting executed")
                    }
                }
            } else if (nearlyEqual && !hasAlreadyGuessed) {
                if (username == currentDrawerUsername) {
                    guessMessage.guessText = guessWord
                    guessAdapter.add(guessMessage)
                    Log.d("GuessingCorrect", "nearlu eequal if getting executed")
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                } else {
                    guessMessage.guessText = "$username's guess is nearly correct!"
                    guessAdapter.add(guessMessage)
                    guessMessage.textColor = Color.parseColor("#F6BE1F")
                    nearlyEqual = false
                    Log.d("GuessingCorrect", "nearly equal   else getting executed")
                    chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                }
            } else {
                guessMessage.guessText = guessWord
                guessAdapter.add(guessMessage)
                chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
                Log.d("GuessingCorrect", "correctword  else getting executed")
            }
        } else {
            guessMessage.guessText = guessWord
            guessAdapter.add(guessMessage)
            chat_log_recyclerview.scrollToPosition(guessAdapter.itemCount - 1)
            Log.d("GuessingCorrect", "timerfinished else  else getting executed")

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
                "playerUid" to myUid,
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

        guess_editText.text?.clear()

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
                            player = dc.document.toObject(Player::class.java)
                            playersList.removeAll { it.username == username }
                            playersList.add(player)
                            updateRecyclerView()

                        }
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed city: ${dc.document.data}"
                        )
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

    private fun wordChoosingTimer() {
        timerForChoosingWord = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerRunning = true
            }

            override fun onFinish() {
                timerRunning = false
                autoSelectWord()
            }
        }
        timerForChoosingWord.start()
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

                    if (!currentPlayer?.currentDrawer!!) {
                        someone_choosing_a_word_textview.animation = top_anim
                        someone_choosing_a_word_textview.visibility = View.VISIBLE
                    } else
                        someone_choosing_a_word_textview.visibility = View.INVISIBLE

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
                    if (currentPlayer?.currentDrawer!!) {
                        chosen_word_textview.text = chosenWordByDrawer
                    } else {
                        text = ""
                        val l = chosenWordByDrawer.length
                        for (i in 0..l - 1) {
                            text += "_ "
                        }
                        chosen_word_textview.text = text
                    }
                    Log.d(TAG, "Current word: $chosenWordByDrawer")

                } else {
                    Log.d(TAG, "Current data in word : null")
                }
            }
    }

    inner class vibrateExecute : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                v.vibrate(200)
            }
            return "vibrated"
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
                        val intent = Intent(this, GameResultsActivity::class.java)
                        intent.putExtra(ROOM_CODE, roomCode)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(ROOM_CODE, roomCode)
                        startActivity(intent)
//                        this.overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
                    }
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    private fun activateBoard() {
        val segment = DrawSegment()
        val scale = 1.0f
        segment.addColor(Color.BLACK)

        segment.addPoint(243, 0)
        segment.addPoint(252, 0)
        segment.addPoint(260, 0)

        segment.addStrokeWidth(10f)
        myPaintView?.invalidate()
        myPaintView?.canvas!!.drawPath(
            PaintView.getPathForPoints(segment.points, scale),
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
        val first = random.nextInt(6800) + 1
        val second = random.nextInt(6800) + 1
        val third = random.nextInt(6800) + 1
        first_word_textview.text = WordsArray.words[first]
        second_word_textview.text = WordsArray.words[second]
        third_word_textview.text = WordsArray.words[third]
    }

    private fun colorPaletteDisplay() {
        val mColor: Int = myPaintView?.paint?.color ?: Color.RED

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

    private fun updateRecyclerView() {
        leaderboard_recyclerview.scheduleLayoutAnimation()
        var colorCount = -1
        playersList.sort()
        if (leaderboardAdapter.itemCount > 0) {
            leaderboardAdapter.clear()
        }
        for (player in playersList) {
            ++colorCount
            val bColor = colorArray[colorCount]
            val item = LeaderboardItem(player, bColor)
            leaderboardAdapter.add(item)
        }

    }

    private fun resetCorrectGuessNum() {
        val ref = db.collection("rooms").document(roomCode)
            .collection("correctGuess")

        ref
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                    val docRef = db.collection("rooms").document(roomCode)
                        .collection("correctGuess").document(document.id)
                    docRef
                        .delete()

                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    private fun listenCorrectGuessNum() {
        val ref = db.collection("rooms").document(roomCode)
            .collection("correctGuess")

        ref
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d(TAG, "New city: ${dc.document.data}")
                            ++count
                            if (count == maxPlayersNum - 1) {
                                if (currentPlayer?.currentDrawer!!) {
                                    currentDrawerTimer.cancel()
                                    currentDrawerTimer.onFinish()
                                } else {
                                    otherPlayerTimer?.cancel()
                                    otherPlayerTimer?.onFinish()
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> Log.d(
                            TAG,
                            "Modified city: ${dc.document.data}"
                        )
                        DocumentChange.Type.REMOVED -> Log.d(
                            TAG,
                            "Removed city: ${dc.document.data}"
                        )
                    }
                }
            }
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
}


