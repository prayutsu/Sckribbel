package com.prayutsu.sckribbel.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    private var _doubleBackToExitPressedOnce = false

    companion object {
        var currentUserLogin: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        back_to_signup_textview.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }

        button_login.setOnClickListener {
            button_login.isEnabled = false
            performLogin()
        }
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

    private fun performLogin() {
        val email = email_login.text.toString()
        val password = password_login.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out email/pw.", Toast.LENGTH_SHORT).show()
            button_login.isEnabled = true
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                login_progressbar.visibility = View.VISIBLE

                Log.d("Login", "Successfully logged in : ${it.result?.user?.uid}")

                val db = Firebase.firestore
                val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()
                currentUserLogin = User("", "", "")


                db.collection("users").document("$uid")
                    .get()
                    .addOnSuccessListener { document ->

                        currentUserLogin?.username = document.getString("username").toString()
                        currentUserLogin?.uid = document.getString("uid").toString()
                        currentUserLogin?.profileImageUrl =
                            document.getString("profileImageUrl").toString()

                        val intent = Intent(this, RoomActivity::class.java)
                        intent.putExtra(USER_KEY_SIGNUP, currentUserLogin)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    .addOnFailureListener { exception ->
                        Log.d("Room", "Error getting documents: $exception")
                        button_login.isEnabled = true
                    }
            }

            .addOnFailureListener {
                Toast.makeText(this, "Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
                button_login.isEnabled = true
            }
    }
}
