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

package com.mugames.vidsnap.utility.bundles;

import static com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel.downloadDetailsList;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.utility.DownloadReceiver;
import com.mugames.vidsnap.utility.UtilityClass;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Random;

/**
 * keeps information while proceeding towards download
 */

public class DownloadDetails implements Parcelable {

    public int id;

    public Uri pathUri;
    public String fileName;
    public String fileType;
    public String srcUrl;
    public String src;
    public int videoQualityIndex;

    public String videoURL;
    public String audioURL;

    public String fileMime;
    public String mimeAudio;

    public String chunkUrl;
    public long chunkCount;

    public long videoSize;
    public long audioSize;

    private Uri thumbNailPath;
    int thumbWidth;
    int thumbHeight;

    public ResultReceiver receiver;

    public boolean isShareOnlyDownload;


    public DownloadDetails() {
    }

    protected DownloadDetails(Parcel in) {
        id = in.readInt();

        pathUri = Uri.parse(in.readString());
        fileName = in.readString();
        fileType = in.readString();
        srcUrl = in.readString();
        src = in.readString();
        videoQualityIndex = in.readInt();

        videoURL = in.readString();
        audioURL = in.readString();

        fileMime = in.readString();
        mimeAudio = in.readString();

        chunkUrl = in.readString();
        chunkCount = in.readLong();

        videoSize = in.readLong();
        audioSize = in.readLong();

        thumbNailPath = Uri.parse(in.readString());
        thumbWidth = in.readInt();
        thumbHeight = in.readInt();

        receiver = in.readParcelable(DownloadReceiver.class.getClassLoader());

        isShareOnlyDownload = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);

        dest.writeString(pathUri.toString());
        dest.writeString(fileName);
        dest.writeString(fileType);
        dest.writeString(srcUrl);
        dest.writeString(src);
        dest.writeInt(videoQualityIndex);

        dest.writeString(videoURL);
        dest.writeString(audioURL);

        dest.writeString(fileMime);
        dest.writeString(mimeAudio);

        dest.writeString(chunkUrl);
        dest.writeLong(chunkCount);

        dest.writeLong(videoSize);
        dest.writeLong(audioSize);

        dest.writeString(thumbNailPath.toString());
        dest.writeInt(thumbWidth);
        dest.writeInt(thumbHeight);

        dest.writeParcelable(receiver, flags);

        dest.writeByte((byte) (isShareOnlyDownload ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DownloadDetails> CREATOR = new Creator<DownloadDetails>() {
        @Override
        public DownloadDetails createFromParcel(Parcel in) {
            return new DownloadDetails(in);
        }

        @Override
        public DownloadDetails[] newArray(int size) {
            return new DownloadDetails[size];
        }
    };

    public Bitmap getThumbNail() {
        return UtilityClass.bytesToBitmap(FileUtil.loadImage(thumbNailPath.getPath()), thumbWidth, thumbHeight);
    }

    public void setThumbNail(Context context, Bitmap resource) {
        Random random = new Random();
        String path = FileUtil.getValidFile(AppPref.getInstance(context).getCachePath(AppPref.DYNAMIC_CACHE), String.valueOf(random.nextLong()), "jpg");
        FileUtil.saveFile(path, UtilityClass.bitmapToBytes(resource), null);
        thumbNailPath = Uri.fromFile(new File(path));
        thumbWidth = resource.getWidth();
        thumbHeight = resource.getHeight();
    }

    public void deleteThumbnail() {
        FileUtil.deleteFile(thumbNailPath.getPath(), null);
    }

    @NonNull
    public static DownloadDetails findDetails(int id) {
        for (DownloadDetails details :
                downloadDetailsList) {
            if (details.id == id) return details;
        }
        throw new IllegalArgumentException("id: "+id+" has no details\nAvailable are"+downloadDetailsList);
    }

    @Nullable
    public static DownloadDetails findDetailsOrNull(int id){
        try{
            return findDetails(id);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            return null;
        }
    }
}
