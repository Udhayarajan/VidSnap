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

package com.mugames.vidsnap.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;


public class MiniExecute {
    String TAG = Statics.TAG + ":MiniExecute";

    Bundle bundle;

    CountDownLatch countDownLatch;
    Bitmap bitmap;

    long size;

    public MiniExecute(Bundle bundle, CountDownLatch countDownLatch) {
        this(bundle);
        this.countDownLatch = countDownLatch;
    }

    public MiniExecute(Bundle bundle) {
        this.bundle = bundle;
    }


    public void getSize(String url, UtilityInterface.SizeCallback sizeCallback) {
        url = url.replaceAll("\\\\", "");
        String finalUrl = url;
        new Thread(() -> {
            sizeCallback.onReceiveSize(calculateSize(finalUrl), bundle);
        }).start();
    }

    public void getSize(String url){
        new Thread(() ->{
            size = calculateSize(url);
            countDownLatch.countDown();
        }).start() ;
    }

    public long getSize() {
        return size;
    }

    public Bundle getBundle() {
        return bundle;
    }

    private long calculateSize(String url) {

        HttpURLConnection conn = null;

        try {
            URL url1 = new URL(url);
            conn = (HttpURLConnection) url1.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    public void getThumbnail(String url) {
        url = url.replaceAll("\\\\", "");
        String finalUrl = url;
        new Thread(() -> {
            try {
                URL url1 = new URL(finalUrl);

                URLConnection urlConnection = url1.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                countDownLatch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}