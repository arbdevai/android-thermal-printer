package com.thermalprinter.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import com.google.gson.Gson
import com.thermalprinter.app.domain.model.StoreSettings
import com.thermalprinter.app.domain.model.TransactionReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
class AppData(private val context: Context) {
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

    suspend fun saveSettings(settings: StoreSettings) {
        store.edit { it[key] = gson.toJson(settings) }
    }

    suspend fun saveLogoFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext ""

            // Scale down if larger than 384px width for thermal printer optimization
            val targetWidth = originalBitmap.width.coerceAtMost(384)
            val targetHeight = ((originalBitmap.height.toFloat() / originalBitmap.width) * targetWidth).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val logoFile = File(context.filesDir, "store_logo.png")
            val outputStream = FileOutputStream(logoFile)
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
            outputStream.flush()
            outputStream.close()

            val current = settings.first()
            saveSettings(current.copy(logoPath = logoFile.absolutePath, showLogo = true))
            logoFile.absolutePath
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun removeLogo() = withContext(Dispatchers.IO) {
        try {
            val logoFile = File(context.filesDir, "store_logo.png")
            if (logoFile.exists()) {
                logoFile.delete()
            }
            val current = settings.first()
            saveSettings(current.copy(logoPath = "", showLogo = false))
        } catch (_: Exception) {}
    }

    fun loadLogoBitmap(path: String): Bitmap? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun save(receipt: TransactionReceipt): Long = db.receipts().save(
        ReceiptRow(receipt.id, receipt.createdAt, gson.toJson(receipt.copy(totalAmount = receipt.calculateTotal())))
    )

    suspend fun delete(id: Long) = db.receipts().delete(id)
    suspend fun clear() = db.receipts().clear()
}
