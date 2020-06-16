package com.prayutsu.sckribbel.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity
import kotlinx.android.synthetic.main.activity_splash.*
//import org.jetbrains.anko.startActivity
import java.util.*

class SplashActivity : AppCompatActivity() {

    companion object {
        var currentUserSplash: User? = null
//        val USER_KEY_LOGIN = "USER_KEY_LOGIN"
    }

    lateinit var fade_in: Animation
    lateinit var fade_out: Animation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        fade_in = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        logo_splash.animation = fade_in
        prayutsu_textview.animation = fade_in
        doodle_imageview.animation = fade_in

        startApp()

    }

    private fun startRoomActivity() {
        val db = Firebase.firestore
        val uid = FirebaseAuth.getInstance().uid
        val randomCode = UUID.randomUUID().toString().substring(0, 15)
        currentUserSplash = User("", "", "")

        val query =
            db.collection("users").document("$uid")
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d("RoomSplash", "${document.id} => ${document.data}")
                        currentUserSplash?.username = document.getString("username").toString()
                        currentUserSplash?.uid = document.getString("uid").toString()
                        currentUserSplash?.profileImageUrl =
                            document.getString("profileImageUrl").toString()
//                            currentUserSplash = document.toObject(User::class.java)
                        Log.d("RoomSplashCurrent", "$currentUserSplash")
                        val intent = Intent(this, RoomActivity::class.java)
                        Log.d("RoomSplash", "Starting roomactivity")
                        intent.putExtra(USER_KEY_SIGNUP, currentUserSplash)
                        startActivity(intent)
                        finish()
                    }
                }

                .addOnFailureListener {
                    Log.d("RoomSplash", "get failed with ")
                }
    }

    private fun startApp() {
        if (FirebaseAuth.getInstance().uid != null) {
            object : CountDownTimer(5000, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    val timeLeft = (millisUntilFinished / 1000).toInt()
                    if (timeLeft == 2
                    ) {
                        logo_splash.animation = fade_out
                        prayutsu_textview.animation = fade_out
                        doodle_imageview.animation = fade_out
                        logo_splash.visibility = View.INVISIBLE
                        doodle_imageview.visibility = View.INVISIBLE
                        prayutsu_textview.visibility = View.INVISIBLE
                        startRoomActivity()

                    }
                }

                override fun onFinish() {

                }

            }.start()


        } else {
            object : CountDownTimer(5000, 1000) {
                override fun onFinish() {
                    startSignupActivity()
                }

                override fun onTick(millisUntilFinished: Long) {
                    var timeLeft = (millisUntilFinished / 1000).toInt()
                    if (timeLeft == 1) {
                        logo_splash.animation = fade_out
                        prayutsu_textview.animation = fade_out
                        doodle_imageview.animation = fade_out
                        logo_splash.visibility = View.INVISIBLE
                        doodle_imageview.visibility = View.INVISIBLE
                        prayutsu_textview.visibility = View.INVISIBLE

                    }
                }

            }.start()

        }
    }

    private fun startSignupActivity() {
        val intent = Intent(applicationContext, SignupActivity::class.java)
        startActivity(intent)
        finish()
    }
}
