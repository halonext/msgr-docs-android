package com.example.dataroomkotlin

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dataroomkotlin.DB.Dao.UserDao
import com.example.dataroomkotlin.DB.Dao.UserDatabase
import com.example.dataroomkotlin.DB.Entity.User
import com.example.dataroomkotlin.DB.Repository.UserRepository
import com.example.dataroomkotlin.databinding.ActivityMainBinding
import com.example.dataroomkotlin.model.MyRecyclerViewAdapter
import com.example.dataroomkotlin.model.MyViewHolder
import com.example.dataroomkotlin.model.UserViewModel
import com.example.dataroomkotlin.model.UserViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userViewModel: UserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        initView()
        btn_save_update.setOnClickListener {
            userViewModel.saveOrUpdate()
            reset()
        }

        btn_clear_delete.setOnClickListener {
            userViewModel.clearAllOrDelete()
        }


    }

    private fun initView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val userDao: UserDao = UserDatabase.getInstance(application).userDao
        val reposity = UserRepository(userDao)
        val factory = UserViewModelFactory(reposity)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)
        binding.myViewModel = userViewModel
        binding.lifecycleOwner = this
        displayUserList()
        initRecyclerView()
    }

    private fun displayUserList() {
        userViewModel.users.observe(this, Observer {
            Log.i("MAYTAG", it.toString())
            binding.listUserView.adapter =
                MyRecyclerViewAdapter(it, { selectedItem: User -> listItemClicked(selectedItem) })
        })
    }

    private fun reset() {
        // Hide keyboard
        val inputManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun initRecyclerView() {
        binding.listUserView.layoutManager = LinearLayoutManager(this)
        displayUserList()
    }

    private fun listItemClicked(user: User) {
        Toast.makeText(this, "select name is ${user.name}", Toast.LENGTH_SHORT).show()
        userViewModel.initUpdateAndDelete(user)

    }


}