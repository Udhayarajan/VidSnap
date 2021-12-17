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

package com.mugames.vidsnap.database;

import static com.mugames.vidsnap.storage.AppPref.STATIC_CACHE;
import static com.mugames.vidsnap.storage.AppPref.db_name;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.utility.UtilityClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Database(entities = {History.class}, version = 4)
public abstract class HistoryDatabase extends RoomDatabase {


    private static volatile HistoryDatabase instance;

    static Migration migration1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date TEXT, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))");
            Cursor cursor = database.query("SELECT * FROM HISTORY");
            while (cursor.moveToNext()) {
                String value = migrateLogic1_3(cursor, 1, 2, 0);
                database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES(" + value + ")");
            }
            database.execSQL("DROP TABLE HISTORY");
            database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY");
        }
    };


    static Migration migration2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date INTRGER, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))");
            Cursor cursor = database.query("SELECT * FROM HISTORY");
            int i = 0;
            while (cursor.moveToNext()) {
                String value = migrateLogic1_3(cursor, 2, 3, i++);
                database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES(" + value + ")");
            }
            database.execSQL("DROP TABLE HISTORY");
            database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY");
        }
    };

    static Migration migration3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("Alter table history add column source_url TEXT");
        }
    };

    static String migrateLogic1_3(Cursor cursor, int from, int to, int i) {
        int id = cursor.getInt(0);
        String fileName = cursor.getString(1);
        String fileType = cursor.getString(2);
        String src = cursor.getString(3);
        String date = cursor.getString(4);
        String size = cursor.getString(5);
        String uriString = cursor.getString(6);
        if (from == 1 && to == 2) {
            byte[] thumbNail = cursor.getBlob(7);
            int width = cursor.getInt(8);
            int height = cursor.getInt(9);

            fileType = fileType.replaceAll("\\.", "");
            String thumbnailString = UtilityClass.bitmapToString(UtilityClass.bytesToBitmap(thumbNail, width, height));
            return id + ",\"" + fileName + "\",\"" + fileType + "\",\"" + src + "\",\"" + date + "\",\"" + size + "\",\"" + uriString + "\",\"" + thumbnailString + "\"";
        } else if (from == 2 && to == 3) {
            Long lDate = parseForDate(date, i);
            String thumbnailString = cursor.getString(7);

            return id + ",\"" + fileName + "\",\"" + fileType + "\",\"" + src + "\"," + lDate + ",\"" + size + "\",\"" + uriString + "\",\"" + thumbnailString + "\"";
        }
        return null;
    }

    private static Long parseForDate(String date, int index) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date1 = simpleDateFormat.parse(date);
            return date1.getTime() + index;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public abstract HistoryDao historyDao();

    public static HistoryDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (HistoryDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), HistoryDatabase.class, AppPref.getInstance(context).getCachePath(STATIC_CACHE) + db_name)
                            .addMigrations(migration1_2, migration2_3, migration3_4)
                            .build();
                }
            }
        }
        return instance;
    }
}
