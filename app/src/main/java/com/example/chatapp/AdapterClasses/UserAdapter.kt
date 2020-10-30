package com.example.chatapp.AdapterClasses

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.MessageActivity
import com.example.chatapp.ModelClasses.Chat
import com.example.chatapp.ModelClasses.Users
import com.example.chatapp.R
import com.example.chatapp.VisitProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
              mContext: Context,
              mUsers: List<Users>,
              isChatCheck:Boolean
   ): RecyclerView.Adapter<UserAdapter.ViewHolder?>()
{
    private val mContext: Context
    private val mUsers: List<Users>
    private val isChatCheck: Boolean
    var lastMsg: String = ""

    init {
        this.mUsers = mUsers
        this.mContext = mContext
        this.isChatCheck = isChatCheck
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder
    {
        val view: View = LayoutInflater.from(mContext)!!.inflate(R.layout.user_search_item_layout, viewGroup,false)
        return UserAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int
    {
        return mUsers.size
    }


    override fun onBindViewHolder(holder: ViewHolder, i: Int)
    {
        val user: Users? = mUsers[i]
        holder.userNameTxt.text = user!!.getUserName()
        Picasso.get().load(user.getProfile()).placeholder(R.drawable.profile).into(holder.profileImageView)

        if(isChatCheck){
            retrieveLastMessage(user.getUid(), holder.lastMessageText)
        }
        else{
            holder.lastMessageText.visibility = View.GONE
        }

        if(isChatCheck){
            if(user.getStatus() == "online"){
                holder.onlineImageText.visibility = View.VISIBLE
                holder.offlineImageText.visibility = View.GONE
            }
            else{
                holder.onlineImageText.visibility = View.GONE
                holder.offlineImageText.visibility = View.VISIBLE
            }
        }else{
            holder.onlineImageText.visibility = View.GONE
            holder.offlineImageText.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            val options = arrayOf<CharSequence>(
                "Send Message",
                "Visit Profile"
            )
            val builder: AlertDialog.Builder = AlertDialog.Builder(mContext)
            builder.setTitle("What do you want")
            builder.setItems(options, DialogInterface.OnClickListener { dialog, position ->
                if (position == 0)
                {
                    val intent = Intent(mContext, MessageActivity::class.java)
                    intent.putExtra("visit_id", user.getUid())
                    mContext.startActivity(intent)
                }
                if (position == 1) {
                    val intent = Intent(mContext, VisitProfileActivity::class.java)
                    intent.putExtra("visit_id", user.getUid())
                    mContext.startActivity(intent)
                }
            })
            builder.show()
        }
    }

    private fun retrieveLastMessage(chatUserId: String?, lastMessageText: TextView) {
        lastMsg = "defaultMsg"
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val refrence = FirebaseDatabase.getInstance().reference.child("Chats")
        refrence.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                for (dataSnapshot in p0.children){
                    val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                    if(firebaseUser!=null && chat!=null){
                        if(chat.getReceiver() == firebaseUser!!.uid && chat.getSender() == chatUserId || chat.getReceiver()==chatUserId && chat.getSender() == firebaseUser!!.uid){
                            lastMsg = chat.getMessage()!!
                        }
                    }
                }
                when(lastMsg){
                    "defaultMsg" -> lastMessageText.text = "No Message "
                    "sent you an image" ->lastMessageText.text = "image sent"
                    else -> lastMessageText.text = lastMsg
                }
                lastMsg = "defaultMsg"
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    class ViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
        var userNameTxt: TextView
        var profileImageView: CircleImageView
        var onlineImageText: CircleImageView
        var offlineImageText: CircleImageView
        var lastMessageText: TextView

        init {
            userNameTxt = itemView.findViewById(R.id.username)
            profileImageView = itemView.findViewById(R.id.profile_image)
            onlineImageText = itemView.findViewById(R.id.image_online)
            offlineImageText = itemView.findViewById(R.id.image_offline)
            lastMessageText = itemView.findViewById(R.id.message_last)

        }

    }

}




