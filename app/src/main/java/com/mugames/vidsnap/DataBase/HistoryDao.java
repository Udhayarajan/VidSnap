package com.mugames.vidsnap.DataBase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface HistoryDao {

    @Insert
    void insertItem(History history);

    @Query("DELETE FROM HISTORY")
    void deleteTable();

    @Query("SELECT * FROM HISTORY")
    LiveData<List<History>> getAllValues();
}
