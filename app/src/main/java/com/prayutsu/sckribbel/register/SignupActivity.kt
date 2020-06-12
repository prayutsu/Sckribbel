package com.prayutsu.sckribbel.register

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.prayutsu.sckribbel.R
import com.prayutsu.sckribbel.model.User
import com.prayutsu.sckribbel.room.RoomActivity
import com.squareup.picasso.Picasso
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_signup.*
import java.io.File
import java.util.*


class SignupActivity : AppCompatActivity() {

    companion object {

        val USER_KEY_SIGNUP = "USER_KEY_SIGNUP"

        var currentUser: User? = null

    }

    var photoSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        already_registered_textview.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        profile_button_signup.setOnClickListener {
            Log.d("MainActivity", "Try to show photo selector")

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)

        }

        button_signup.setOnClickListener {
            if (photoSelected) {
                button_signup.isEnabled = false
                performSignup()
            } else
                Toast.makeText(this, "Setting up a profile photo is necessary!", Toast.LENGTH_SHORT)
                    .show()
        }


    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            //proceed and check what the selected image was..
            Log.d("MainActivity", "The photo was selected")

            selectedPhotoUri = data.data

            Picasso.get().load(selectedPhotoUri).into(profile_photo_imageView)

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            profile_photo_imageView.setImageBitmap(bitmap)

            profile_button_signup.visibility = View.GONE
            photoSelected = true


        }
    }

    private fun performSignup() {
        val email = email_signup.text.toString()
        val password = password_signup.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            button_signup.isEnabled = true
            Log.d("Main", "Please enter some text in both the fields")
            Toast.makeText(this, "Please enter some text in email/password", Toast.LENGTH_SHORT)
                .show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                //else if successful
                Log.d("Main", "Successfully created user with uid : ${it.result?.user?.uid}")

                progressBar_signup.visibility = View.VISIBLE
                Blurry.with(this).capture(layout).into(imageView)


                progressBar_signup.visibility = View.VISIBLE
                uploadImageToFirebaseStore()
            }

            .addOnFailureListener {
                Log.d("Main", "Registration failed!! : ${it.message}")
                Toast.makeText(this, "Failed to create user : ${it.message}", Toast.LENGTH_SHORT)
                    .show()
                button_signup.isEnabled = true
            }

    }



    private fun uploadImageToFirebaseStore() {
        if (selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("Main", "Successfully uploaded photo : ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    it.toString()
                    Log.d("Main", "File location : $it")

                    saveUserToFirebaseFirestore(it.toString())
                }
            }

            .addOnFailureListener {
                Log.d("Main", "Photo was not uploaded due to some error : ${it.message}")
                Toast.makeText(this, "Couldn't upload photo : ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveUserToFirebaseFirestore(profileImageUrl: String) {
        val db = Firebase.firestore
        val username = username_signup.text.toString()
        val uid = FirebaseAuth.getInstance().uid

        val user = uid?.let { User(it, username, profileImageUrl) }

        currentUser = user

        if (user != null) {
            db.collection("users").document("$uid")
                .set(user)
                .addOnSuccessListener {
                    Log.d("Signup", "DocumentSnapshot written successfully")

                    val intent = Intent(this, RoomActivity::class.java)
                    intent.putExtra(USER_KEY_SIGNUP, currentUser)
                    //clear all other activities from stack by using the  below line
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Log.d("Signup", "Error adding document")
                }
        }

    }


}
