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

package com.mugames.vidsnap.Utility.Bundles;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.mugames.vidsnap.Utility.DownloadReceiver;

import java.util.ArrayList;



public class DownloadDetails implements Parcelable {

    public Uri pathUri;
    public String fileName;
    public String fileType;
    public String originalUrl;
    public String src;

    public String videoURL;
    public String audioURL;

    public String mimeVideo;
    public String mimeAudio;

    public ArrayList<String> m3u8URL;//Chuncks will be stored if null get from saved path chuncks path
    public String chunksPath;
    public String chunkUrl;
    public long chunkCount;

    public long videoSize;
    public long audioSize;


    public Bitmap thumbNail;
    public String thumbNailPath;
    public int thumbWidth;
    public int thumbHeight;

    public ResultReceiver receiver;


    public DownloadDetails() {}

    protected DownloadDetails(Parcel in) {
        pathUri = Uri.parse(in.readString());
        fileName = in.readString();
        fileType = in.readString();
        originalUrl = in.readString();
        src = in.readString();

        videoURL = in.readString();
        audioURL = in.readString();

        mimeVideo = in.readString();
        mimeAudio = in.readString();

        m3u8URL = in.createStringArrayList();
        chunksPath = in.readString();
        chunkUrl = in.readString();
        chunkCount = in.readLong();

        videoSize = in.readLong();
        audioSize = in.readLong();

        thumbNail = in.readParcelable(Bitmap.class.getClassLoader());
        thumbNailPath = in.readString();
        thumbWidth = in.readInt();
        thumbHeight = in.readInt();

        receiver = in.readParcelable(DownloadReceiver.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pathUri.toString());
        dest.writeString(fileName);
        dest.writeString(fileType);
        dest.writeString(originalUrl);
        dest.writeString(src);

        dest.writeString(videoURL);
        dest.writeString(audioURL);

        dest.writeString(mimeVideo);
        dest.writeString(mimeAudio);

        dest.writeStringList(m3u8URL);
        dest.writeString(chunksPath);
        dest.writeString(chunkUrl);
        dest.writeLong(chunkCount);

        dest.writeLong(videoSize);
        dest.writeLong(audioSize);

        dest.writeParcelable(thumbNail, flags);
        dest.writeString(thumbNailPath);
        dest.writeInt(thumbWidth);
        dest.writeInt(thumbHeight);

        dest.writeParcelable(receiver,flags);
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
}
