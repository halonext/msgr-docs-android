package com.example.dataroomkotlin.DB.Repository

import com.example.dataroomkotlin.DB.Dao.UserDao
import com.example.dataroomkotlin.DB.Entity.User

class UserRepository(private val userDao: UserDao) {

    val users = userDao.getAllUsers()

    suspend fun insert(user: User) {
        userDao.insertUser(user)
    }

    suspend fun update(user: User) {
        userDao.updateUser(user)
    }

    suspend fun delete(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteAll() {
        userDao.deleteALlUsers()
    }

}