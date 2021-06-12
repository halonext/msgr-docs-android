package com.example.dataroomkotlin.model

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.dataroomkotlin.DB.Entity.User
import com.example.dataroomkotlin.DB.Repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel(), Observable {

    val users = repository.users
    private var isUpdateOrDelete = false
    private lateinit var userToUpdateOrDelete: User

    @Bindable
    val inputName = MutableLiveData<String>()

    @Bindable
    val inputEmail = MutableLiveData<String>()

    @Bindable
    val inputPassword = MutableLiveData<String>()

    @Bindable
    val saveOrUpdate = MutableLiveData<String>()

    @Bindable
    val clearAllOrDelete = MutableLiveData<String>()

    init {
        saveOrUpdate.value = "Save"
        clearAllOrDelete.value = "Clear All"
    }

    fun saveOrUpdate() {
        if (isUpdateOrDelete) {
            userToUpdateOrDelete.name = inputName.value!!
            userToUpdateOrDelete.email = inputEmail.value!!
            userToUpdateOrDelete.password = inputPassword.value!!
                update(userToUpdateOrDelete)
        } else {
            val name: String = inputName.value!!
            val email: String = inputEmail.value!!
            val password: String = inputPassword.value!!
            insert(User(id = 0, name, email, password))
            inputName.value = null
            inputEmail.value = null
            inputPassword.value = null
        }
    }

    fun clearAllOrDelete() {
        if (isUpdateOrDelete) {
            delete(userToUpdateOrDelete)
        } else {
            clearAll()
        }

    }

    fun insert(user: User): Job = viewModelScope.launch {

        repository.insert(user)
    }

    private fun update(user: User): Job = viewModelScope.launch {
        repository.update(user)
        inputName.value = null
        inputEmail.value = null
        inputPassword.value = null
        isUpdateOrDelete = false
        saveOrUpdate.value = "Save"
        clearAllOrDelete.value = "Clear All"
    }

    private fun delete(user: User): Job = viewModelScope.launch {
        repository.delete(user)
        inputName.value = null
        inputEmail.value = null
        inputPassword.value = null
        isUpdateOrDelete = false
        saveOrUpdate.value = "Save"
        clearAllOrDelete.value = "Clear All"
    }

    private fun clearAll(): Job = viewModelScope.launch {
        repository.deleteAll()

    }

    fun initUpdateAndDelete(user: User) {
        inputName.value = user.name
        inputEmail.value = user.email
        inputPassword.value = user.password
        isUpdateOrDelete = true
        userToUpdateOrDelete = user
        saveOrUpdate.value = "Update"
        clearAllOrDelete.value = "Delete"
    }


    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }


    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }


}

