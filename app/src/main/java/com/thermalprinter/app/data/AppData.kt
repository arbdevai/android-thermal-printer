package com.thermalprinter.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import com.google.gson.Gson
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "receipts")
data class ReceiptRow(@PrimaryKey(autoGenerate = true) val id: Long = 0, val createdAt: Long, val payload: String)

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun observe(): Flow<List<ReceiptRow>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: ReceiptRow): Long
    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM receipts")
    suspend fun clear()
}

@Database(entities = [ReceiptRow::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receipts(): ReceiptDao
}

private val Context.settingsStore by preferencesDataStore("store_settings")
class AppData(context: Context) {
    private val gson = Gson()
    private val store = context.applicationContext.settingsStore
    private val key = stringPreferencesKey("settings")
    val db = Room.databaseBuilder(context.applicationContext, ReceiptDatabase::class.java, "receipts.db").build()
    val settings: Flow<StoreSettings> = store.data.map { prefs ->
        prefs[key]?.let { runCatching { gson.fromJson(it, StoreSettings::class.java) }.getOrNull() } ?: StoreSettings()
    }
    val receipts: Flow<List<TransactionReceipt>> = db.receipts().observe().map { rows ->
        rows.map { gson.fromJson(it.payload, TransactionReceipt::class.java).copy(id = it.id) }
    }
    suspend fun saveSettings(settings: StoreSettings) { store.edit { it[key] = gson.toJson(settings) } }
    suspend fun save(receipt: TransactionReceipt): Long = db.receipts().save(
        ReceiptRow(receipt.id, receipt.createdAt, gson.toJson(receipt.copy(totalAmount = receipt.calculateTotal())))
    )
    suspend fun delete(id: Long) = db.receipts().delete(id)
    suspend fun clear() = db.receipts().clear()
}
