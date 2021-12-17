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

import android.net.Uri;
import android.text.format.DateFormat;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.UtilityClass;

import java.util.Date;

@Entity(tableName = "HISTORY")
@TypeConverters(DateConverter.class)
public class History {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fileName;
    public String fileType;
    public String source;
    public Date date;
    public String size;
    public String uriString;
    public String image;
    @ColumnInfo(name = "source_url")
    public String sourceUrl;
    public History() {}


    public History(DownloadDetails details, Uri uri) {
        this.fileName = details.fileName;
        this.fileType = details.fileType;
        this.source = details.src;
        this.date = DateConverter.toDate( new Date().getTime());
        this.size = String.valueOf(details.videoSize);
        this.uriString = uri.toString();
        sourceUrl = details.srcUrl;
        image = UtilityClass.bitmapToString(details.getThumbNail());
    }

    public Uri getUri(){
        return Uri.parse(uriString);
    }

    public String getDate() {
        return (String) DateFormat.format("dd-MM-yyyy", date);
    }
}
