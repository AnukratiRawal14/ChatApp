package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_register.*


class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var refUsers: DatabaseReference
    private var firebaseUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // setting toolbar and back button so that it can navigate to main screen
        val toolbar: Toolbar = findViewById(R.id.toolbar_register)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Register"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this@RegisterActivity, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        mAuth = FirebaseAuth.getInstance()
        register_btn.setOnClickListener {
            registerUser()
        }

    }

    private fun registerUser() {
        val username: String = username_register.text.toString()
        val email: String = email_register.text.toString()
        val password: String = password_register.text.toString()

        if (username == "")
        {
            Toast.makeText(this@RegisterActivity, "Enter your username", Toast.LENGTH_LONG).show()
        }
        else if (email == "")
        {
            Toast.makeText(this@RegisterActivity, "Enter your Email-Id", Toast.LENGTH_LONG).show()
        }
        else if (password == "")
        {
            Toast.makeText(this@RegisterActivity, "Enter Password", Toast.LENGTH_LONG).show()
        }
        else
        {
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        firebaseUserId = mAuth.currentUser!!.uid
                        refUsers = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUserId)

                        val userHashMap = HashMap<String, Any>()
                        userHashMap["Uid"] = firebaseUserId
                        userHashMap["UserName"] = username
                        userHashMap["profile"] = "https://firebasestorage.googleapis.com/v0/b/chatapp-27257.appspot.com/o/profile.png?alt=media&token=7c5d5573-8efc-4cc8-a50d-742cc1b29c0c"
                        userHashMap["cover"] = "https://firebasestorage.googleapis.com/v0/b/chatapp-27257.appspot.com/o/cover.jpg?alt=media&token=2e3bad13-b970-469f-aa05-49b6bd125521"
                        userHashMap["status"] = "offline"
                        userHashMap["search"] = username.toLowerCase()
                        userHashMap["facebook"] = "https://m.facebook.com"
                        userHashMap["instagram"] = "https://m.instagram.com"
                        userHashMap["website"] = "https://www.google.com"

                        //making reference and passing hashMap value
                        refUsers.updateChildren(userHashMap).addOnCompleteListener { task ->
                            if (task.isSuccessful)
                            {
                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                    else {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Error Message" + task.exception!!.message.toString(),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
        }
    }
}
