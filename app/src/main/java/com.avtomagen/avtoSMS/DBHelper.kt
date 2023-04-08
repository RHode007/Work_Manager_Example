package com.avtomagen.avtoSMS

import androidx.lifecycle.LiveData
import androidx.room.*


@Entity
data class PhoneNumber(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "status") val status: String?,
    @ColumnInfo(name = "number") val number: String?,
    @ColumnInfo(name = "text") val text: String?
)

@Entity
data class User(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "api") val api: String?,
    @ColumnInfo(name = "debugAccess") val debugAccess: Int = 0,
    @ColumnInfo(name = "logging") val logging: Int = 0,
)

@Entity
data class Logger(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "text") val text: String?,
)

@Database(entities = [PhoneNumber::class, User::class, Logger::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phoneNumberDao(): PhoneNumberListDao
    abstract fun userDao(): UserDao
    abstract fun loggerDao(): LoggerDao
}

@Dao
interface PhoneNumberListDao {
    @Query("SELECT * FROM phonenumber")
    fun getAll(): List<PhoneNumber>

    @Query("SELECT * FROM phonenumber WHERE id IN (:phoneNumberIds)")
    fun loadAllByIds(phoneNumberIds: IntArray): List<PhoneNumber>

    @Query("SELECT * FROM phonenumber WHERE number LIKE :number LIMIT 1")
    fun findByNumber(number: String): PhoneNumber

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg phoneNumbers: PhoneNumber)

    @Delete
    fun delete(phoneNumber: PhoneNumber)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg user: User)

    @Query("UPDATE user SET debugAccess = :debugAccess WHERE id = :id")
    fun updateDebugAccess(id: Int, debugAccess: Int): Int

    @Query("UPDATE user SET logging = :logging WHERE id = :id")
    fun updateLogging(id: Int, logging: Int): Int

    @Query("SELECT api FROM user LIMIT 1")
    fun getApi(): String

    @Query("SELECT * FROM user WHERE id = :id")
    fun getById(id: Int): User

    @Delete
    fun delete(user: User)
}

@Dao
interface LoggerDao {
    @Query("SELECT COUNT(*) FROM logger")
    fun count(): Int

    @Query("SELECT * FROM logger")
    fun getAll(): LiveData<List<Logger>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(logger: Logger)

    @Delete
    fun delete(logger: Logger)

    @Query("DELETE FROM Logger")
    fun deleteAll()
}