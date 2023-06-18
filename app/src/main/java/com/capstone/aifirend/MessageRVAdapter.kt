package com.capstone.aifirend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageRVAdapter(private val messageList: ArrayList<MessageRVModal>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val userMsgTV: TextView = itemView.findViewById(R.id.idTVUser)
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val botMsgTV: TextView = itemView.findViewById(R.id.idTVBot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        lateinit var view: View
        return if(viewType == 0) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.user_message_rv_item, parent, false)
            UserMessageViewHolder(view)
        } else {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.bot_message_rv_item, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sender = messageList[position].sender
        when (sender) {
            "user" -> (holder as UserMessageViewHolder).userMsgTV.text = messageList[position].message
            "bot" -> (holder as BotMessageViewHolder).botMsgTV.text = messageList[position].message
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(messageList[position].sender) {
            "user" -> 0
            "bot" -> 1
            else -> 1
        }
    }
}