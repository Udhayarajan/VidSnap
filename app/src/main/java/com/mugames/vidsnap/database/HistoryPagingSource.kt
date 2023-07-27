/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.database

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max


/**
 * @author Udhaya
 * Created on 25-07-2023
 */

private const val STARTING_KEY = 0

class HistoryPagingSource(private val historyDao: HistoryDao) : PagingSource<Int, History>() {
    // The refresh key is used for the initial load of the next PagingSource, after invalidation
    override fun getRefreshKey(state: PagingState<Int, History>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val history = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(key = history.id - (state.config.pageSize / 2))
    }

    private fun ensureValidKey(key: Int) = max(STARTING_KEY, key)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, History> {
        // If params.key is null, it is the first load, so we start loading with STARTING_KEY
        val startKey = params.key ?: STARTING_KEY
        val data = historyDao.paginatedHistory(params.loadSize, startKey) ?: emptyList()

        return LoadResult.Page(
            data = data,
            prevKey = when (startKey) {
                STARTING_KEY -> null
                else -> when (val prevKey = ensureValidKey(key = startKey - params.loadSize)) {
                    // We're at the start, there's nothing more to load
                    STARTING_KEY -> null
                    else -> prevKey
                }
            },
            nextKey = if (data.isEmpty()) null else startKey + params.loadSize
        )
    }
}