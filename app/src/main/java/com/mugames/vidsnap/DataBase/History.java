package com.mugames.vidsnap.DataBase;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.UtilityClass;

@Entity(tableName = "HISTORY")
public class History {

    @PrimaryKey(autoGenerate = true)
    int id;
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
        this.size = String.valueOf(details.fileSize);
        this.uriString = uri.toString();
        this.thumbnail = (byte[]) FileUtil.loadFile(details.thumbNailPath,byte.class);
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
