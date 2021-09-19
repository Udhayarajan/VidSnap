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

package com.mugames.vidsnap.DataBase;

import static com.mugames.vidsnap.ui.ViewModels.MainActivityViewModel.STATIC_CACHE;
import static com.mugames.vidsnap.ui.ViewModels.MainActivityViewModel.db_name;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mugames.vidsnap.Utility.AppPref;
import com.mugames.vidsnap.Utility.UtilityClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Database(entities = {History.class}, version = 3)
public abstract class HistoryDatabase extends RoomDatabase {


    private static volatile HistoryDatabase instance;

    static Migration migration1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date TEXT, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))");
            Cursor cursor = database.query("SELECT * FROM HISTORY");
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String fileName = cursor.getString(1);
                String fileType = cursor.getString(2);
                String src = cursor.getString(3);
                String date = cursor.getString(4);
                String size = cursor.getString(5);
                String uriString = cursor.getString(6);
                byte[] thumbNail = cursor.getBlob(7);
                int width = cursor.getInt(8);
                int height = cursor.getInt(9);

                fileType = fileType.replaceAll("\\.", "");
                String thumbnailString = UtilityClass.bitmapToString(UtilityClass.bytesToBitmap(thumbNail, width, height));
                String value = id + ",\"" + fileName + "\",\"" + fileType + "\",\"" + src + "\",\"" + date + "\",\"" + size + "\",\"" + uriString + "\",\"" + thumbnailString + "\"";
                database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES(" + value + ")");
            }
            database.execSQL("DROP TABLE HISTORY");
            database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY");
        }
    };

    static Migration migration1_3 = new Migration(1, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE HISTORY_NEW (id INTEGER NOT NULL , fileName TEXT, fileType TEXT, source TEXT, date INTEGER, size TEXT, uriString TEXT, image TEXT, PRIMARY KEY(id))");
            Cursor cursor = database.query("SELECT * FROM HISTORY");
            int i = 0;
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String fileName = cursor.getString(1);
                String fileType = cursor.getString(2);
                String src = cursor.getString(3);
                String date = cursor.getString(4);
                String size = cursor.getString(5);
                String uriString = cursor.getString(6);
                byte[] thumbNail = cursor.getBlob(7);
                int width = cursor.getInt(8);
                int height = cursor.getInt(9);

                fileType = fileType.replaceAll("\\.", "");
                String thumbnailString = UtilityClass.bitmapToString(UtilityClass.bytesToBitmap(thumbNail, width, height));
                Long lDate = parseForDate(date, ++i);
                String value = id + ",\"" + fileName + "\",\"" + fileType + "\",\"" + src + "\"," + lDate + ",\"" + size + "\",\"" + uriString + "\",\"" + thumbnailString + "\"";
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
                int id = cursor.getInt(0);
                String fileName = cursor.getString(1);
                String fileType = cursor.getString(2);
                String src = cursor.getString(3);
                String date = cursor.getString(4);
                Long lDate = parseForDate(date, i++);
                String size = cursor.getString(5);
                String uriString = cursor.getString(6);
                String thumbnailString = cursor.getString(7);
                String value = id + ",\"" + fileName + "\",\"" + fileType + "\",\"" + src + "\"," + lDate + ",\"" + size + "\",\"" + uriString + "\",\"" + thumbnailString + "\"";
                database.execSQL("INSERT INTO HISTORY_NEW (id,fileName,fileType,source,date,size,uriString,image) VALUES(" + value + ")");
            }
            database.execSQL("DROP TABLE HISTORY");
            database.execSQL("ALTER TABLE HISTORY_NEW RENAME TO HISTORY");
        }
    };

    private static Long parseForDate(String date, int index) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
                            .addMigrations(migration1_2, migration2_3, migration1_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
