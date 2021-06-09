package com.example.wschat


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            10
        )

        enterBtn.setOnClickListener {
            if (editText.text.isNotEmpty()) {

                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("name", editText.text.toString())
                startActivity(intent)

            } else {
                Toast.makeText(applicationContext, "Name should not be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}