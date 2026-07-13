package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "fall_logs")
data class FallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val aiResult: String, // "정상", "낙상 의심", "응급상황 가능성 높음"
    val userResponse: String, // "괜찮습니다", "119 신고 완료", "무응답 대응"
    val isSimulated: Boolean = false,
    val maxAccel: Float = 0f
)

@Dao
interface FallLogDao {
    @Query("SELECT * FROM fall_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<FallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FallLog)

    @Query("DELETE FROM fall_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM fall_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}

@Database(entities = [FallLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fallLogDao(): FallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fall_detector_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class FallRepository(private val fallLogDao: FallLogDao) {
    val allLogs: Flow<List<FallLog>> = fallLogDao.getAllLogs()

    suspend fun insert(log: FallLog) {
        fallLogDao.insertLog(log)
    }

    suspend fun clear() {
        fallLogDao.clearAllLogs()
    }

    suspend fun delete(id: Int) {
        fallLogDao.deleteLogById(id)
    }
}
