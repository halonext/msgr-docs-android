package com.example.dataroomkotlin.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.dataroomkotlin.DB.Entity.User
import com.example.dataroomkotlin.R

import com.example.dataroomkotlin.databinding.ListItemBinding

class MyRecyclerViewAdapter(
    private val user: List<User>,
    private val clickListener: (User) -> Unit
) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: ListItemBinding =
            DataBindingUtil.inflate(layoutInflater, R.layout.list_item, parent, false)
        return MyViewHolder(binding)
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(user[position],clickListener)
    }


    override fun getItemCount(): Int {
        return user.size
    }
}

class MyViewHolder(private val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(user: User, clickListener: (User) -> Unit) {
        binding.txtNameView.text = user.name
        binding.txtEmailView.text = user.email
        binding.txtPasswordView.text = user.password
        binding.listItemLayout.setOnClickListener {
            clickListener(user)
        }
    }
}