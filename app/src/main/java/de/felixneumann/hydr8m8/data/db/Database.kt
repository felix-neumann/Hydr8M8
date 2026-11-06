package de.felixneumann.hydr8m8.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMillis: Long,
    val kind: String,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: String,
    val builtInKey: String?,
    val hidden: Boolean,
    val sortOrder: Int,
    @ColumnInfo(defaultValue = "NULL") val name: String? = null,
)

/** Ordered preset selection for one surface (home, shortcuts, notification, widget:<id>). */
@Entity(tableName = "surface_slots", primaryKeys = ["surface", "position"])
data class SurfaceSlotEntity(
    val surface: String,
    val position: Int,
    val presetId: Long,
)

@Entity(tableName = "goal_changes")
data class GoalChangeEntity(
    @PrimaryKey val effectiveFromEpochDay: Long,
    val goalMl: Int,
)

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val epochDay: Long,
    val maxTempC: Double,
    val bonusMl: Int,
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val appWidgetId: Int,
    val showMate: Boolean,
    val showInfo: Boolean,
    val showUndo: Boolean,
    val tapPresetId: Long?,
    val background: String,
    val opacityPercent: Int,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY atMillis")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY atMillis")
    suspend fun all(): List<EntryEntity>

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun byId(id: Long): EntryEntity?

    @Query("SELECT * FROM entries WHERE createdAtMillis >= :sinceMillis ORDER BY createdAtMillis DESC, id DESC LIMIT 1")
    suspend fun lastCreatedSince(sinceMillis: Long): EntryEntity?

    @Query("SELECT MAX(atMillis) FROM entries WHERE atMillis <= :nowMillis")
    suspend fun lastEntryAt(nowMillis: Long): Long?
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY sortOrder, id")
    suspend fun all(): List<PresetEntity>

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int

    @Insert
    suspend fun insert(preset: PresetEntity): Long

    @Update
    suspend fun update(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id AND builtInKey IS NULL")
    suspend fun deleteCustom(id: Long)

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun byId(id: Long): PresetEntity?
}

@Dao
interface SurfaceDao {
    @Query("SELECT * FROM surface_slots ORDER BY surface, position")
    fun observeAll(): Flow<List<SurfaceSlotEntity>>

    @Query("SELECT * FROM surface_slots WHERE surface = :surface ORDER BY position")
    suspend fun slots(surface: String): List<SurfaceSlotEntity>

    @Query("DELETE FROM surface_slots WHERE surface = :surface")
    suspend fun clear(surface: String)

    @Query("DELETE FROM surface_slots WHERE presetId = :presetId")
    suspend fun removePreset(presetId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<SurfaceSlotEntity>)

    @Transaction
    suspend fun replace(
        surface: String,
        presetIds: List<Long>,
    ) {
        clear(surface)
        insertAll(presetIds.mapIndexed { i, id -> SurfaceSlotEntity(surface, i, id) })
    }
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_changes ORDER BY effectiveFromEpochDay")
    fun observeAll(): Flow<List<GoalChangeEntity>>

    @Query("SELECT * FROM goal_changes ORDER BY effectiveFromEpochDay")
    suspend fun all(): List<GoalChangeEntity>

    @Upsert
    suspend fun upsert(change: GoalChangeEntity)
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather")
    fun observeAll(): Flow<List<WeatherEntity>>

    @Query("SELECT * FROM weather")
    suspend fun all(): List<WeatherEntity>

    @Query("SELECT * FROM weather WHERE epochDay = :epochDay")
    suspend fun byDay(epochDay: Long): WeatherEntity?

    @Upsert
    suspend fun upsert(weather: WeatherEntity)
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :id")
    suspend fun byId(id: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs")
    fun observeAll(): Flow<List<WidgetConfigEntity>>

    @Upsert
    suspend fun upsert(config: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE appWidgetId = :id")
    suspend fun delete(id: Int)
}

@Database(
    entities = [
        EntryEntity::class,
        PresetEntity::class,
        SurfaceSlotEntity::class,
        GoalChangeEntity::class,
        WeatherEntity::class,
        WidgetConfigEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao

    abstract fun presets(): PresetDao

    abstract fun surfaces(): SurfaceDao

    abstract fun goals(): GoalDao

    abstract fun weather(): WeatherDao

    abstract fun widgetConfigs(): WidgetConfigDao

    companion object {
        const val NAME = "hydr8m8.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
