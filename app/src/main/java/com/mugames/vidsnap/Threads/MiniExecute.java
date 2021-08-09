package com.mugames.vidsnap.Threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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



public class MiniExecute extends Thread {
    String TAG= Statics.TAG+":MiniExecute";
    MainActivity activity;
    URL url;
    boolean isSize;
    int position;
    UtilityInterface.MiniExecutorCallBack callBack;
    public MiniExecute(MainActivity activity, String url, boolean size, int position, UtilityInterface.MiniExecutorCallBack callBack){
        Log.d(TAG, "video : MiniExecute: created"+size+" url : "+url);
        this.activity = activity;
        this.callBack=callBack;
        this.position=position;
        url=url.replaceAll("\\\\","");
        isSize=size;
        try {
            this.url=new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MiniExecute: "+url,e);
        }
    }

    @Override
    public void run() {
        if(isSize)
            getFileSize();
        else FetchThumbnail();
    }

    private void FetchThumbnail() {
        try {
            Log.e(TAG, "FetchThumbnail: "+url.toString());
            URLConnection urlConnection=url.openConnection();
            InputStream inputStream=urlConnection.getInputStream();
            Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callBack.onBitmapReceive(bitmap);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    int tries=3;

    public void getFileSize() {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.setDoOutput(false);
            int size = conn.getContentLength();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callBack.onBitmapReceive(null);
                    callBack.onSizeReceived(size,position);
                }
            });
        } catch (IOException e) {
//            tries--;
//            if(tries<0)
            throw new RuntimeException(e);
 //           getFileSize();
        }
    }
}