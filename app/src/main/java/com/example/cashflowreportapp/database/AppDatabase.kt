package com.example.cashflowreportapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashflow_database"
                )
                    .fallbackToDestructiveMigration() // safely rebuilds DB if schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
