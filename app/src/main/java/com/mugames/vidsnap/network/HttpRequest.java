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

import android.util.Log;

import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityInterface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.net.ssl.HttpsURLConnection;



public class HttpRequest{
    String TAG= Statics.TAG+":HttpRequest";
    public static final String POST="POST";
    public static final String GET = "GET";
    URL info_url;
    UtilityInterface.ResponseCallBack callBack;
    String cookies;
    Hashtable<String,String> headers=new Hashtable<>();
    String type;
    String data;

    public HttpRequest(MainActivity activity, String videoURL, String cookies, Hashtable<String,String> headers, String type, String data, String user_agent, UtilityInterface.ResponseCallBack callBack){
        try {
            this.cookies=cookies;
            this.callBack=callBack;
            this.headers=headers;
            this.type=type;
            this.data=data;
            info_url=new URL(videoURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            activity.error("URL seems to be incorrect",e);
        }
    }

    public HttpRequest(String webpageURL, UtilityInterface.ResponseCallBack callBack){
        try {
            this.info_url = new URL(webpageURL);
            this.callBack = callBack;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setHeaders(Hashtable<String, String> headers) {
        this.headers = headers;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public void start(){
        new Thread(()->{
            try {
                //Sending Request to  sever
                HttpsURLConnection httpsURLConnection= getConnection(info_url.toString());


                // normally, 3xx is redirect

                while (isRedirected(httpsURLConnection)){
                    String newUrl = httpsURLConnection.getHeaderField("Location");
                    httpsURLConnection.disconnect();
                    Log.d(TAG, "start: "+newUrl);
                    httpsURLConnection = null;
                    httpsURLConnection = getConnection(newUrl);
                    httpsURLConnection.connect();
                }

                InputStream stream = httpsURLConnection.getInputStream();


                ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
                byte[] buffers=new byte[8192];
                int length=0;
                while  ((length=stream.read(buffers))!=-1){
                    outputStream.write(buffers,0,length);
                }
//                BufferedReader in = new BufferedReader(
//                        new InputStreamReader(httpsURLConnection.getInputStream()));
//                String inputLine;
//                StringBuilder html = new StringBuilder();
//
//                while ((inputLine = in.readLine()) != null) {
//                    html.append(inputLine);
//                }
//                in.close();

                stream.close();
                callBack.onReceive(new Response(outputStream.toString()));

            } catch (IOException e) {
                e.printStackTrace();
                callBack.onReceive(new Response(e));
            }
        }).start();
    }

    private HttpsURLConnection getConnection(String url) throws IOException {

        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(url).openConnection();

        if(headers!=null){
            Enumeration<String> keys=headers.keys();
            while (keys.hasMoreElements()){
                String s = keys.nextElement();
                httpsURLConnection.setRequestProperty(s,headers.get(s));
            }

        }


        httpsURLConnection.setRequestProperty("Accept-Language", "en-GB");
        httpsURLConnection.setRequestProperty("Content-Language", "en-GB");

        if(type!=null && type.equals(POST)){
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setRequestMethod("POST");
        }
        else {
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setInstanceFollowRedirects(false);
        }

        if(data!=null){
            OutputStream outputStream = httpsURLConnection.getOutputStream();
            outputStream.write(data.getBytes());
            outputStream.close();
        }

        if(cookies!=null)
            httpsURLConnection.setRequestProperty("Cookie",cookies);

        Log.d(TAG, "run: "+(httpsURLConnection.getRequestProperties()).get("User-Agent"));

        return httpsURLConnection;
    }

    boolean isRedirected(HttpsURLConnection httpsURLConnection) throws IOException {
        int status = httpsURLConnection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            return status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER;
        }
        return false;
    }

}
