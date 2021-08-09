package com.mugames.vidsnap.DataBase;

import com.mugames.vidsnap.Utility.Bundles.HistoryDetails;

import static com.mugames.vidsnap.Utility.UtilityClass.bitmapToBytes;

public class DataBundle {
    public String name;
    public String src;
    public String date;
    public long size;
    public String uri;
    public byte[] thumbnail;
    public Long width;
    public Long height;



    public DataBundle(HistoryDetails tempDetails) {
        this.name = tempDetails.getDownloadDetails().fileName;
        this.src = tempDetails.getDownloadDetails().src;
        this.date = tempDetails.getDate();
        this.size = tempDetails.getDownloadDetails().fileSize;
        this.uri = String.valueOf(tempDetails.getUri());
        this.thumbnail = bitmapToBytes(tempDetails.getDownloadDetails().thumbNail);
        this.width = (long) tempDetails.getDownloadDetails().thumbNail.getWidth();
        this.height =  (long) tempDetails.getDownloadDetails().thumbNail.getHeight();
    }
}
