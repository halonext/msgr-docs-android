package com.example.wschat.ultil

import com.google.gson.Gson


object MyUltil {

    var gson = Gson()
    fun encode(msg: SocketData): String? {
        var jsonString = gson.toJson(msg)
        return jsonString
    }


    fun decode(byteArray: ByteArray): String {
        val stringValue = byteArray.decodeToString()
        return stringValue
    }

    fun encodeUT(input: String): ByteArray {
        return input.encodeToByteArray()
    }

}

