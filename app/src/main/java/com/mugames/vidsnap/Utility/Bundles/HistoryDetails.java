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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class HistoryDetails implements Parcelable {

    /**
     * Used for array list of history fragment's recycler view
     */
    DownloadDetails details;
    Uri uri;
    String date;

    public HistoryDetails(DownloadDetails details, Uri uri, String date) {
        this.details = details;
        this.uri = uri;
        this.date = date;
    }


    protected HistoryDetails(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        date = in.readString();
    }

    public static final Creator<HistoryDetails> CREATOR = new Creator<HistoryDetails>() {
        @Override
        public HistoryDetails createFromParcel(Parcel in) {
            return new HistoryDetails(in);
        }

        @Override
        public HistoryDetails[] newArray(int size) {
            return new HistoryDetails[size];
        }
    };

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    public DownloadDetails getDownloadDetails() {
        return details;
    }

    public void setDetails(DownloadDetails details) {
        this.details = details;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeString(date);
    }
}