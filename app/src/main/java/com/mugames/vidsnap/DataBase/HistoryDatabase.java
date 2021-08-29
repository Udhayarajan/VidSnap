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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mugames.vidsnap.Utility.AppPref;
import com.mugames.vidsnap.Utility.FileUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.STATIC_CACHE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.db_name;

@Database(entities = {History.class}, version = 1, exportSchema = false)
public abstract class HistoryDatabase extends RoomDatabase {


    private static HistoryDatabase instance;

    public abstract HistoryDao historyDao();

    public static synchronized HistoryDatabase getInstance(Context context){
        if(instance == null){
            Migration migration_1_2 = new Migration(1,2) {
                @Override
                public void migrate(@NonNull @NotNull SupportSQLiteDatabase database) {
                    Cursor cursor = database.query("SELECT imagepath FROM HISTORY");
                    List<String> path = new ArrayList<>();
                    while (cursor.moveToNext()){
                        path.add(cursor.getString(0));
                    }
                    List<byte[]> images = pathToImages(path);
                    database.execSQL("CREATE TABLE HISTORY_TEMP (id INTEGER primary key autoincrement,"+
                            "fileName TEXT, fileType TEXT, source TEXT, date TEXT, size TEXT, uriString TEXT, thumbnail BLOB, imageWidth INTEGER, imageHeight INTEGER )");
                    for (int i = 1; i <= path.size(); i++) {
                        database.execSQL("INSERT INTO HISTORY_TEMP(id,fileName,fileType,source,date,size,uriString,imageWidth,imageHeight) SELECT " +
                                "id,fileName,fileType,source,date,size,uriString,imageWidth,imageHeight FROM HISTORY");
//                        database.execSQL("Update HISTORY_TEMP set thumbnail="+ +" where id="+i);
                        ContentValues cv = new ContentValues();
                        cv.put("thumbnail", images.get(i-1));
                        database.update("HISTORY_TEMP",0, cv, "id is "+i, null);
                    }
                    database.execSQL("DROP TABLE HISTORY");
                    database.execSQL("ALTER TABLE HISTORY_TEMP RENAME TO HISTORY");
                }
            };


            instance = Room.databaseBuilder(context.getApplicationContext(),HistoryDatabase.class,new AppPref(context).getCachePath(STATIC_CACHE)+db_name)
                    .addMigrations(migration_1_2).build();

        }
        return instance;
    }

    private static List<byte[]> pathToImages(List<String> paths) {
        List<byte[]> bytes = new ArrayList<>();
        for (String path : paths) {
            bytes.add((byte[]) FileUtil.loadFile(path,byte[].class));
        }
        return bytes;
    }

    static RoomDatabase.Callback callback = new RoomDatabase.Callback(){
        @Override
        public void onCreate(@NonNull @NotNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }
    };

    private static class PopulateDatabaseAsync{
        HistoryDao historyDao;

        public PopulateDatabaseAsync(HistoryDatabase historyDatabase) {
            this.historyDao = historyDatabase.historyDao();
        }

        public void backgroundTask(){
            Thread thread = new Thread(() -> {

            });
            thread.start();
        }
    }
}
