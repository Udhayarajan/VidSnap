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
package com.mugames.vidsnap.ui.viewmodels

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mugames.vidsnap.database.History
import com.mugames.vidsnap.database.HistoryRepository
import kotlinx.coroutines.flow.Flow

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    var pagedValue: Flow<PagingData<History>> = Pager(
        config = PagingConfig(ITEMS_PER_PAGE, enablePlaceholders = false, initialLoadSize = ITEMS_PER_PAGE),
        pagingSourceFactory = { repository.paginationSource }
    ).flow.cachedIn(viewModelScope)

    private var pendingHistory: History? = null
    private var intentSenderLiveData = MutableLiveData<IntentSender>()


    fun insert(history: History?) {
        repository.insertItem(history)
    }

    fun clearRepository() {
        repository.clear()
    }

    fun deleteThisItem(currentHistory: History) {
        try {
            repository.delete(currentHistory)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (e is RecoverableSecurityException) {
                    pendingHistory = currentHistory
                    intentSenderLiveData.postValue(e.userAction.actionIntent.intentSender)
                    return
                }
            }
            throw e
        }
    }

    val intentSender: LiveData<IntentSender>
        get() = intentSenderLiveData

    fun deletePendingUri(resolver: ContentResolver) {
        if (pendingHistory != null) {
            resolver.delete(pendingHistory!!.uri, null, null)
            deleteThisItem(pendingHistory!!)
            pendingHistory = null
        }
    }

    companion object {
        private const val ITEMS_PER_PAGE = 10
    }
}