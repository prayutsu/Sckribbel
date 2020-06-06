package com.prayutsu.sckribbel.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {

    companion object {
        var currentUserLogin: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



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
