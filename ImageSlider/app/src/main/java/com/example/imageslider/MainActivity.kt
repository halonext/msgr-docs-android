package com.example.imageslider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var viewPager: ViewPager? = null
    var images =
        intArrayOf(R.drawable.image01, R.drawable.image02, R.drawable.image03, R.drawable.image04)
    var myAdapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myAdapter = MyAdapter(this, images)
        viewPagerMain!!.adapter = myAdapter
    }
}