package com.example.wschat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wschat.ultil.Constans
import com.example.wschat.ultil.MyUltil
import com.example.wschat.ultil.PayloadData
import com.example.wschat.ultil.SocketData
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException


var isConnect: Boolean = false

class ChatActivity : AppCompatActivity(), TextWatcher {


    companion object {
        private lateinit var adapter: MessageAdapter
        private lateinit var webSocket: WebSocket
        private var name: String? = null
        private val IMAGE_REQUEST_ID = 1
        private val SERVER_PATH = "wss://msgr-gw-dev.hlapis.com/messenger/v2/gateway"
        var socketData = SocketData()
        var payloadData = PayloadData()
        val gson = Gson()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        name = intent.getStringExtra("name")
        initiateSocketConnection()
        if (isConnect) {
            payloadData.token = Constans.tokenSocket
            socketData.event = Constans.identify
            socketData.payload = payloadData
            val jsonString: String = gson.toJson(socketData)
            val a = MyUltil.encodeUT(jsonString)
            webSocket.send(a.toString())

        }
        intallView()

    }

    private fun intallView() {
        messageList.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter()
        messageList.adapter = adapter
        messageEdit.addTextChangedListener(this)


//        sendBtn.setOnClickListener { v: View? ->
//            var socketData = SocketData()
//            var payloadData = PayloadData()
//            val gson = Gson()
//            try {
////                socketData.name = name.toString()
////                socketData.message = messageEdit.text.toString()
//                payloadData.token = Constans.tokenSocket
//                socketData.event = Constans.identify
//                socketData.payload = payloadData
//
//                val jsonString: String = gson.toJson(socketData)
//
//                webSocket.send(jsonString)
//
//            } catch (e: JSONException) {
//                e.printStackTrace()
//            }
//        }
//
//        pickImgBtn.setOnClickListener { v: View? ->
//            val intent = Intent(Intent.ACTION_GET_CONTENT)
//            intent.type = "image/*"
//            startActivityForResult(
//                Intent.createChooser(intent, "Pick image"),
//                IMAGE_REQUEST_ID
//            )
//        }
    }

    private fun resetMessageEdit() {
        // Clean text box
        messageEdit.text.clear()

        sendBtn.visibility = View.INVISIBLE
        pickImgBtn.visibility = View.VISIBLE

        // Hide keyboard
        val inputManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }


    private fun initiateSocketConnection() {
        val client = OkHttpClient()
        val request = Request.Builder().url(SERVER_PATH).build()
        webSocket = client.newWebSocket(request, SocketListener())
    }

    inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            runOnUiThread(Runnable {
                Toast.makeText(
                    this@ChatActivity,
                    "Socket Connection Successful!",
                    Toast.LENGTH_SHORT
                ).show()

            })
            isConnect = true
        }


//        override fun onMessage(webSocket: WebSocket, text: String) {
//            super.onMessage(webSocket, text)
//
//            runOnUiThread(Runnable {
//                try {
//                    val jsonObject = JSONObject(text)
//                    adapter.addItem(jsonObject)
//                    messageList.smoothScrollToPosition(adapter.getItemCount() - 1)
//
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            })
//        }

//        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//            super.onMessage(webSocket, bytes)
//
//        }



//        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//            super.onClosed(webSocket, code, reason)
//            runOnUiThread(Runnable {
//                Toast.makeText(
//                    this@ChatActivity,
//                    "Socket Connection Closed !",
//                    Toast.LENGTH_SHORT
//                ).show()
//            })
//        }
    }


    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }


    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }


    override fun afterTextChanged(s: Editable?) {

        val string = s.toString().trim { it <= ' ' }

        if (string.isEmpty()) {
            resetMessageEdit()
        } else {
            sendBtn.visibility = View.VISIBLE
            pickImgBtn.visibility = View.INVISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_ID && resultCode == RESULT_OK) {
            try {
                val `is` = contentResolver.openInputStream(data!!.data!!)
                val image = BitmapFactory.decodeStream(`is`)
                sendImage(image)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendImage(image: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val base64String = Base64.encodeToString(
            outputStream.toByteArray(),
            Base64.DEFAULT
        )
        val jsonObject = JSONObject()
        try {
            jsonObject.put("name", name)
            jsonObject.put("image", base64String)
            webSocket.send(jsonObject.toString())
            jsonObject.put("isSent", true)
            adapter.addItem(jsonObject)
            messageList.smoothScrollToPosition(adapter.getItemCount() - 1)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


}










