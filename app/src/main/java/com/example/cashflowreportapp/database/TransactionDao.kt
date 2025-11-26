package com.example.cashflowreportapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Update
import androidx.room.Delete

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :transactionType")
    fun getTotalAmountByType(transactionType: String): LiveData<Double?>

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE account = :accountName")
    suspend fun getTransactionsByAccountSync(accountName: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE account = :accountName ORDER BY date DESC")
    fun getTransactionsByAccount(accountName: String): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDate(type: String, startDate: String, endDate: String): Double?

    @Delete
    suspend fun delete(transaction: Transaction)
}
