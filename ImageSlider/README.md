# Step 1: Create a New Project
# Step 2:  Working with the activity_main.xml file

Navigate to the app > res > layout > activity_main.xml and add the below code to that file. Below is the code for the activity_main.xml file. 
```
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity" >

    <!-- viewpager to show images -->
    <androidx.viewpager.widget.ViewPager
        android:id="@+id/viewPagerMain"
        android:background="@drawable/custom_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</RelativeLayout>
```
# Step 3: Create a new  item.xml in layout. Below is the code for the item.xml file. 
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/imageView"/>


</LinearLayout>
```
# Step 4: Create a new  MyAdapter.kl . Below is the code for the MyAdapter.kl file. 
```
  package com.example.imageslider

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter

class MyAdapter(var context: Context, var images: IntArray) : PagerAdapter() {

    lateinit var layoutInflater: LayoutInflater

    override fun getCount(): Int {
        return images.size
    }


    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as LinearLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = layoutInflater.inflate(R.layout.item, container, false)

        val imageView = view.findViewById<View>(R.id.imageView) as ImageView

        imageView.setImageResource(images[position])
        container.addView(view)
        imageView.setOnClickListener {
            Toast.makeText(context, "Images" + (position + 1), Toast.LENGTH_LONG).show()
        }
        return view
    }

    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as LinearLayout)
    }

}

```
# Step 5: Edit MainActivity.kl . Below is the code for the MainActivity.kl file and Run
```
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
```

