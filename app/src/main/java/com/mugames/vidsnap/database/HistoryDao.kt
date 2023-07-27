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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert
    fun insertItem(history: History)

    @Query("DELETE FROM HISTORY")
    fun deleteTable()

    @Query("SELECT COUNT(1) FROM HISTORY")
    fun isEntryAvailable(): LiveData<Boolean?>?

    @Query("SELECT * from HISTORY ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun paginatedHistory(limit: Int, offset: Int): List<History>?

    @Delete
    fun removeItem(history: History)
}