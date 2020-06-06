package com.prayutsu.sckribbel.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.register.SignupActivity.Companion.USER_KEY_SIGNUP
import com.prayutsu.sckribbel.room.RoomActivity
//import org.jetbrains.anko.startActivity
import java.util.*

class SplashActivity : AppCompatActivity() {

    companion object {
        var currentUserSplash: User? = null
//        val USER_KEY_LOGIN = "USER_KEY_LOGIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().uid != null) {

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

        } else {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
