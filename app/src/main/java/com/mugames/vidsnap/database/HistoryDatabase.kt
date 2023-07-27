/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mugames.vidsnap.database

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mugames.vidsnap.storage.AppPref
import com.mugames.vidsnap.utility.UtilityClass
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

@Database(entities = [History::class], version = 4)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: HistoryDatabase? = null
        var migration1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date TEXT, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))")
                val cursor = database.query("SELECT * FROM HISTORY")
                while (cursor.moveToNext()) {
                    val value = migrateLogic1_3(cursor, 1, 2, 0)
                    database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES($value)")
                }
                database.execSQL("DROP TABLE HISTORY")
                database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY")
            }
        }
        var migration2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date INTRGER, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))")
                val cursor = database.query("SELECT * FROM HISTORY")
                var i = 0
                while (cursor.moveToNext()) {
                    val value = migrateLogic1_3(cursor, 2, 3, i++)
                    database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES($value)")
                }
                database.execSQL("DROP TABLE HISTORY")
                database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY")
            }
        }
        var migration3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Alter table history add column source_url TEXT")
            }
        }

        fun migrateLogic1_3(cursor: Cursor, from: Int, to: Int, i: Int): String? {
            val id = cursor.getInt(0)
            val fileName = cursor.getString(1)
            var fileType = cursor.getString(2)
            val src = cursor.getString(3)
            val date = cursor.getString(4)
            val size = cursor.getString(5)
            val uriString = cursor.getString(6)
            if (from == 1 && to == 2) {
                val thumbNail = cursor.getBlob(7)
                val width = cursor.getInt(8)
                val height = cursor.getInt(9)
                fileType = fileType.replace("\\.".toRegex(), "")
                val thumbnailString = UtilityClass.bitmapToString(
                    UtilityClass.bytesToBitmap(
                        thumbNail,
                        width,
                        height
                    )
                )
                return "$id,\"$fileName\",\"$fileType\",\"$src\",\"$date\",\"$size\",\"$uriString\",\"$thumbnailString\""
            } else if (from == 2 && to == 3) {
                val lDate = parseForDate(date, i)
                val thumbnailString = cursor.getString(7)
                return "$id,\"$fileName\",\"$fileType\",\"$src\",$lDate,\"$size\",\"$uriString\",\"$thumbnailString\""
            }
            return null
        }

        private fun parseForDate(date: String, index: Int): Long? {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val date1 = simpleDateFormat.parse(date)
                if (date1 != null) {
                    return date1.time + index
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return null
        }

        @JvmStatic
        fun getInstance(context: Context): HistoryDatabase {
            return instance ?: synchronized(this) {
                val instance = databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    AppPref.getInstance(context)
                        .getCachePath(AppPref.STATIC_CACHE) + AppPref.db_name
                )
                    .addMigrations(migration1_2, migration2_3, migration3_4)
                    .build()

                this.instance = instance
                instance
            }
        }
    }
}