package com.example.chatapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.AdapterClasses.ChatAdapter
import com.example.chatapp.Fragments.APIService
import com.example.chatapp.ModelClasses.Chat
import com.example.chatapp.ModelClasses.Users
import com.example.chatapp.Notifications.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_message.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MessageActivity : AppCompatActivity()
{
    var userIdVisit: String = ""
    var firebaseUser: FirebaseUser? = null
    var chatAdapter: ChatAdapter? = null
    var mChatList: List<Chat>? = null
    lateinit var receiver_chat: RecyclerView
    var reference: DatabaseReference? = null
    var notify = false
    var apiService: APIService? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_chat)
        setSupportActionBar(toolbar)

        supportActionBar!!.title = ""
        apiService = Client.Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        intent = intent
        userIdVisit = intent.getStringExtra("visit_id")!!
        firebaseUser = FirebaseAuth.getInstance().currentUser

        receiver_chat = findViewById(R.id.recycler_chat)
        receiver_chat.setHasFixedSize(true)

        var linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        receiver_chat.layoutManager = linearLayoutManager

        //displaying username and profile image
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(userIdVisit)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user: Users? = p0.getValue(Users::class.java)
                username_chat.text = user!!.getUserName()
                Picasso.get().load(user.getProfile()).into(profile_image_chat)

                //retrieve message
                retrieveMessage(firebaseUser!!.uid, userIdVisit, user.getProfile())
            }

            override fun onCancelled(p0: DatabaseError) {}
        })

        // when user click on send button
        send_msg_btn.setOnClickListener {
            notify = true
            val message = text_message.text.toString()
            if (message == "")
            {
                Toast.makeText(
                    this@MessageActivity,
                    "Please write message first...",
                    Toast.LENGTH_LONG
                ).show()
            }
            else
            {
                sendMessageToUser(firebaseUser!!.uid, userIdVisit, message)
            }
            text_message.setText("")
        }

        // when user click on attach button
        attach_image_file_btn.setOnClickListener {
            notify = true
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Pick Image"), 438)
        }
        seenMessage(userIdVisit)
    }

    private fun sendMessageToUser(senderId: String, receiverId: String?, message: String) {
        //saving message to database
        val reference = FirebaseDatabase.getInstance().reference
        val messageKey = reference.push().key

        val messageHashMap = HashMap<String, Any?>()
        messageHashMap["sender"] = senderId
        messageHashMap["message"] = message
        messageHashMap["receiver"] = receiverId
        messageHashMap["isseen"] = false
        messageHashMap["url"] = ""
        messageHashMap["messageId"] = messageKey
        reference.child("Chats")
            .child(messageKey!!)
            .setValue(messageHashMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful)
                {
                    val chatListReference = FirebaseDatabase.getInstance().reference
                    val commandRef = chatListReference.child("ChatList")
                        .child(firebaseUser!!.uid)
                        .child(userIdVisit)

                    commandRef.addListenerForSingleValueEvent(object : ValueEventListener
                    {
                        override fun onDataChange(p0: DataSnapshot)
                        {
                            if (!p0.exists()) {
                                commandRef.child("id").setValue(userIdVisit)
                            }
                            val chatListReceiverRef = FirebaseDatabase.getInstance().reference
                            val commandRef1 = chatListReceiverRef.child("ChatList")
                                .child(userIdVisit)
                                .child(firebaseUser!!.uid)

                            commandRef1.child("id").setValue(firebaseUser!!.uid)
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })

                }
            }

        //implement the push notification
        val userReference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
        userReference.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                val user = p0.getValue(Users::class.java)
                if (notify)
                {
                    sendNotification(receiverId, user!!.getUserName(), message)
                }
                notify = false
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }




    private fun sendNotification(receiverId: String?, UserName: String?, message: String?) {
        val ref = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = ref.orderByKey().equalTo(receiverId)
        query.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                for (dataSnapshot in p0.children)
                {
                    val token: Token? = dataSnapshot.getValue(Token::class.java)

                    val data = Data( firebaseUser!!.uid, R.mipmap.ic_launcher, "$UserName:$message", "New Message", userIdVisit)

                    val sender = Sender(data!!, token!!.getToken().toString())

                    apiService!!.sendNotification(sender)
                        .enqueue(object : Callback<MyResponse>
                        {
                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                if (response.code() == 200)
                                {
                                    if (response.body()!!.success !== 1)
                                    {
                                        Toast.makeText(this@MessageActivity, "wait....", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            override fun onFailure(
                                call: Call<MyResponse>,
                                t: Throwable
                            ) {
                            }
                        })

                }

            }

            override fun onCancelled(p0: DatabaseError) {}
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 438 && resultCode == Activity.RESULT_OK && data != null && data!!.data != null)
        {
            val progressBar = ProgressDialog(this)
            progressBar.setMessage("image is uploading please wait...")
            progressBar.show()

            val fileUri = data.data
            val storageReference = FirebaseStorage.getInstance().reference.child("Chat Images")
            val ref = FirebaseDatabase.getInstance().reference
            val messageId = ref.push().key
            val filePath = storageReference.child("$messageId.jpg")

            var uploadTask: StorageTask<*>
            uploadTask = filePath.putFile(fileUri!!)

            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (task.isSuccessful)
                {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation filePath.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful)
                {
                    val downloadUrl = task.result
                    val url = downloadUrl.toString()

                    // saving to database
                    val messageHashMap = HashMap<String, Any?>()
                    messageHashMap["sender"] = firebaseUser!!.uid
                    messageHashMap["message"] = "Sent you an image"
                    messageHashMap["receiver"] = userIdVisit
                    messageHashMap["isseen"] = false
                    messageHashMap["url"] = url
                    messageHashMap["messageId"] = messageId

                    ref.child("Chats").child(messageId!!).setValue(messageHashMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful)
                            {
                                progressBar.dismiss()
                                //implement the push notifications using fcm
                                val reference = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser!!.uid)
                                reference.addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(p0: DataSnapshot)
                                    {
                                        val user = p0.getValue(Users::class.java)
                                        if (notify)
                                        {
                                            sendNotification(userIdVisit, user!!.getUserName(), "Sent you an image")
                                        }
                                        notify = false
                                    }
                                    override fun onCancelled(p0: DatabaseError) {}
                                })
                            }
                        }
                }
            }
        }
    }


    private fun retrieveMessage(
        senderId: String,
        receiverId: String?,
        receiverImageUrl: String?
    ) {
        mChatList = ArrayList()
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        reference!!.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                (mChatList as ArrayList<Chat>).clear()
                for (snapshot in p0.children)
                {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat!!.getReceiver()
                            .equals(senderId) && chat.getSender()
                            .equals(receiverId) || chat.getReceiver()
                            .equals(receiverId) && chat.getSender()
                            .equals(senderId)
                    ) {
                        (mChatList as ArrayList<Chat>).add(chat)
                    }
                    chatAdapter = ChatAdapter(
                        this@MessageActivity,
                        (mChatList as ArrayList<Chat>),
                        receiverImageUrl!!
                    )
                    receiver_chat.adapter = chatAdapter
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }

    var seenListener: ValueEventListener? = null

    private fun seenMessage(userId: String) {
        reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot)
            {
                for (dataSnapshot in p0.children)
                {
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat!!.getReceiver()
                            .equals(firebaseUser!!.uid) && chat!!.getSender()
                            .equals(userId)
                       ) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["isseen"] = true
                        dataSnapshot.ref.updateChildren(hashMap)
                    }
                }
            }
            override fun onCancelled(p0: DatabaseError) {}
        })
    }

    override fun onPause()
    {
        super.onPause()
        reference!!.removeEventListener(seenListener!!)
    }
}

