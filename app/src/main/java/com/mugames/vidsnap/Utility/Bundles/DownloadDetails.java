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
    public String chuncksPath;

    public long fileSize;
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
        chuncksPath = in.readString();

        fileSize = in.readLong();
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
        dest.writeString(chuncksPath);

        dest.writeLong(fileSize);
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
