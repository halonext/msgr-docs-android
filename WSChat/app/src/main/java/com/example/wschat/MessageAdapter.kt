package com.example.wschat


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONException
import org.json.JSONObject


public class MessageAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private val messages: ArrayList<JSONObject> = ArrayList()


    companion object {
        private const val TYPE_MESSAGE_SENT = 0
        private const val TYPE_MESSAGE_RECEIVED = 1
        private const val TYPE_IMAGE_SENT = 2
        private const val TYPE_IMAGE_RECEIVED = 3
    }


    private class SentMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var messageTxt: TextView = itemView.findViewById(R.id.sentTxt)

    }

    private class SentImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)

    }

    private class ReceivedMessageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var nameTxt: TextView = itemView.findViewById(R.id.nameTxt)
        var messageTxt: TextView = itemView.findViewById(R.id.receivedTxt)

    }

    private class ReceivedImageHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.imageView)
        var nameTxt: TextView = itemView.findViewById(R.id.nameTxt)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        try {
            return if (message.getBoolean("isSent")) {
                if (message.has("message")) TYPE_MESSAGE_SENT else TYPE_IMAGE_SENT
            } else {
                if (message.has("message")) TYPE_MESSAGE_RECEIVED else TYPE_IMAGE_RECEIVED
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var view: View
        when (viewType) {
            TYPE_MESSAGE_SENT -> {
                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_sent_message, parent, false)
                return SentMessageHolder(view)
            }
            TYPE_MESSAGE_RECEIVED -> {

                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_received_message, parent, false)
                return ReceivedMessageHolder(view)
            }
            TYPE_IMAGE_SENT -> {
                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_sent_image, parent, false)
                return SentImageHolder(view)
            }
            TYPE_IMAGE_RECEIVED -> {
                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_received_photo, parent, false)
                return ReceivedImageHolder(view)
            }
        }
        throw AssertionError("impossible viewType: $viewType")
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val message = messages[position]

        try {
            if (message.getBoolean("isSent")) {
                if (message.has("message")) {
                    val messageHolder = holder as SentMessageHolder
                    messageHolder.messageTxt.text = message.getString("message")
                } else {
                    val imageHolder = holder as SentImageHolder
                    val bitmap: Bitmap = getBitmapFromString(message.getString("image"))
                    imageHolder.imageView.setImageBitmap(bitmap)
                }
            } else {
                if (message.has("message")) {
                    val messageHolder = holder as ReceivedMessageHolder
                    messageHolder.nameTxt.text = message.getString("name")
                    messageHolder.messageTxt.text = message.getString("message")
                } else {
                    val imageHolder = holder as ReceivedImageHolder
                    imageHolder.nameTxt.text = message.getString("name")
                    val bitmap: Bitmap = getBitmapFromString(message.getString("image"))
                    imageHolder.imageView.setImageBitmap(bitmap)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    private fun getBitmapFromString(image: String): Bitmap {
        val bytes = Base64.decode(image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun addItem(jsonObject: JSONObject) {
        messages.add(jsonObject)
        notifyDataSetChanged()
    }
}


