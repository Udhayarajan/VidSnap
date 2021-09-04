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

import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.STATIC_CACHE;
import static com.mugames.vidsnap.ViewModels.MainActivityViewModel.db_name;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mugames.vidsnap.Utility.AppPref;

@Database(entities = {History.class}, version = 1)
public abstract class HistoryDatabase extends RoomDatabase {


    private static volatile HistoryDatabase instance;

    public abstract HistoryDao historyDao();

    public static HistoryDatabase getInstance(Context context){
        if(instance == null){
            synchronized (HistoryDatabase.class){
                if(instance==null){
                    instance = Room.databaseBuilder(context.getApplicationContext(),HistoryDatabase.class,AppPref.getInstance(context).getCachePath(STATIC_CACHE)+db_name)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
