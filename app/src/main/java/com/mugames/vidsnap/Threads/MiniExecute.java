package com.mugames.vidsnap.Threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;


public class MiniExecute {
    String TAG = Statics.TAG + ":MiniExecute";

    Bundle bundle;

    CountDownLatch countDownLatch;

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
        new Thread(() -> sizeCallback.onReceiveSize(calculateSize(finalUrl), bundle)).start();
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


    public void getThumbnail(String url, UtilityInterface.ThumbnailCallbacks thumbnailCallbacks) {
        url = url.replaceAll("\\\\", "");
        String finalUrl = url;
        new Thread(() -> {
            try {
                URL url1 = new URL(finalUrl);

                URLConnection urlConnection = url1.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                thumbnailCallbacks.onReceivedThumbnail(bitmap, bundle);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}