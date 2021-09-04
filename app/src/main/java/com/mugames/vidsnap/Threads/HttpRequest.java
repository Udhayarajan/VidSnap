package com.mugames.vidsnap.Threads;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.net.ssl.HttpsURLConnection;



public class HttpRequest extends Thread {
    String TAG= Statics.TAG+":HttpRequest";
    public static final String POST="POST";
    URL info_url;
    MainActivity activity;
    UtilityInterface.ResponseCallBack callBack;
    String cookies;
    Hashtable<String,String> headers=new Hashtable<>();
    String type;
    String data;
    String user_agent;
    public HttpRequest(MainActivity activity, String videoURL, String cookies, Hashtable<String,String> headers, String type, String data, String user_agent, UtilityInterface.ResponseCallBack callBack){
        try {
            this.cookies=cookies;
            this.callBack=callBack;
            this.headers=headers;
            this.type=type;
            this.data=data;
            this.activity=activity;
            info_url=new URL(videoURL);
            this.user_agent=user_agent;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            activity.error("URL seems to be incorrect",e);
        }
    }

    @Override
    public void run() {

        try {
            //Sending Request to  sever
            HttpsURLConnection httpsURLConnection=(HttpsURLConnection) info_url.openConnection();

            if(headers!=null){
                Enumeration<String> keys=headers.keys();
                while (keys.hasMoreElements()){
                    String s = keys.nextElement();
                    httpsURLConnection.setRequestProperty(s,headers.get(s));
                }

            }

            if(user_agent!=null)
                httpsURLConnection.setRequestProperty("User-Agent",user_agent);

            httpsURLConnection.setRequestProperty("Accept-Language", "en-GB");
            httpsURLConnection.setRequestProperty("Content-Language", "en-GB");

            if(type!=null && type.equals(POST)){
                httpsURLConnection.setDoInput(true);
                httpsURLConnection.setDoOutput(true);
                httpsURLConnection.setRequestMethod("POST");
            }
            else {
                httpsURLConnection.setRequestMethod("GET");
            }

            if(data!=null){
                OutputStream outputStream = httpsURLConnection.getOutputStream();
                outputStream.write(data.getBytes());
                outputStream.close();
            }

            if(cookies!=null)
                httpsURLConnection.setRequestProperty("Cookie",cookies);

            Log.d(TAG, "run: "+(httpsURLConnection.getRequestProperties()).get("User-Agent"));

            InputStream stream = httpsURLConnection.getInputStream();




            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            byte[] buffers=new byte[1024];
            int length=0;
            while  ((length=stream.read(buffers))!=-1){
                outputStream.write(buffers,0,length);
            }

            stream.close();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callBack.onReceive(outputStream.toString());
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(info_url.toString().contains("youtube") || (info_url.toString().contains("instagram") && user_agent==null))
                        callBack.onReceive(null);
                    else {
                        activity.error("Internal error occurred",e);
                    }
                }
            });

        }
    }


}
