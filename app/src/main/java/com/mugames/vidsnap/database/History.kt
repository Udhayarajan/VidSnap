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

import android.net.Uri
import android.text.format.DateFormat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mugames.vidsnap.utility.UtilityClass
import com.mugames.vidsnap.utility.bundles.DownloadDetails
import java.util.Date

@Entity(tableName = "HISTORY")
@TypeConverters(DateConverter::class)
class History {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id = 0
    @JvmField
    var fileName: String? = null
    @JvmField
    var fileType: String? = null
    @JvmField
    var source: String? = null
    @JvmField
    var date: Date? = null
    @JvmField
    var size: String? = null
    @JvmField
    var uriString: String? = null
    @JvmField
    var image: String? = null

    @JvmField
    @ColumnInfo(name = "source_url")
    var sourceUrl: String? = null

    constructor() {}
    constructor(details: DownloadDetails, uri: Uri) {
        fileName = details.fileName
        fileType = details.fileType
        source = details.src
        date = DateConverter.toDate(Date().time)
        size = details.videoSize.toString()
        uriString = uri.toString()
        sourceUrl = details.srcUrl
        image = UtilityClass.bitmapToString(details.thumbNail)
    }

    val uri: Uri
        get() = Uri.parse(uriString)

    fun getDate(): String {
        return DateFormat.format("dd-MM-yyyy", date) as String
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val history = o as History
        return fileName == history.fileName && fileType == history.fileType && source == history.source && date == history.date && size == history.size && uriString == history.uriString && image == history.image && sourceUrl == history.sourceUrl
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (fileType?.hashCode() ?: 0)
        result = 31 * result + (source?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (size?.hashCode() ?: 0)
        result = 31 * result + (uriString?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (sourceUrl?.hashCode() ?: 0)
        return result
    }
}