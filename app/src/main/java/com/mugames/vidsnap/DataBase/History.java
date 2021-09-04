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

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Storage.FileUtil;
import com.mugames.vidsnap.Utility.UtilityClass;

@Entity(tableName = "HISTORY")
public class History {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fileName;
    public String fileType;
    public String source;
    public String date;
    public String size;
    public String uriString;
    public byte[] thumbnail;
    public int imageWidth;
    public int imageHeight;

    public History() {}

    public History(DownloadDetails details, Uri uri, String date) {
        this.fileName = details.fileName;
        this.fileType = details.fileType;
        this.source = details.src;
        this.date = date;
        this.size = String.valueOf(details.videoSize);
        this.uriString = uri.toString();
        new Thread(()->{
            this.thumbnail = (byte[]) FileUtil.loadImage(details.thumbNailPath);
        }).start();
        this.imageWidth = details.thumbWidth;
        this.imageHeight = details.thumbHeight;
    }

    public Bitmap getThumbnail() {
        return UtilityClass.bytesToBitmap(thumbnail,imageWidth,imageHeight);
    }

    public Uri getUri(){
        return Uri.parse(uriString);
    }
}
