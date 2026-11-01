package com.example.sabr_and_iron.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sabr_and_iron.data.local.dao.ExerciseDao
import com.example.sabr_and_iron.data.local.dao.RoutineDao
import com.example.sabr_and_iron.data.local.dao.WorkoutDao
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import com.example.sabr_and_iron.data.local.entity.RoutineExerciseEntity
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SabrIronDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DATABASE_NAME = "sabr_iron.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sets ADD COLUMN durationSeconds INTEGER")
                db.execSQL("ALTER TABLE sets ADD COLUMN distanceMeters REAL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN machine TEXT")
            }
        }

        /** Drop seeded library and sample workouts from earlier builds. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM sets")
                db.execSQL("DELETE FROM workout_exercises")
                db.execSQL("DELETE FROM workouts")
                db.execSQL("DELETE FROM routine_exercises")
                db.execSQL("DELETE FROM routines")
                db.execSQL("DELETE FROM exercises")
            }
        }

        @Volatile
        private var instance: SabrIronDatabase? = null

        fun getInstance(context: Context): SabrIronDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SabrIronDatabase =
            Room.databaseBuilder(context, SabrIronDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
