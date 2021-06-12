package com.example.dataroomkotlin.DB.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dataroomkotlin.DB.Entity.User

@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query(value = "SELECT * FROM user_table")
    fun getAllUsers(): LiveData<List<User>>

    @Query(value = "DELETE FROM user_table")
    fun deleteALlUsers()

}