package com.mugames.vidsnap.Extractor;

import android.graphics.Bitmap;
import android.util.Log;

import com.mugames.vidsnap.Firebase.FirebaseManager;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.Extractor;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.Utility.UtilityClass.JSONGetter.getArray_or_Null;
import static com.mugames.vidsnap.Utility.UtilityClass.JSONGetter.getObj_or_Null;
import static com.mugames.vidsnap.Utility.UtilityClass.JSONGetter.getString_or_Null;
import static com.mugames.vidsnap.Utility.UtilityInterface.*;
import static com.mugames.vidsnap.Utility.UtilityInterface.LoginIdentifier;
import static com.mugames.vidsnap.Utility.UtilityInterface.MiniExecutorCallBack;
import static com.mugames.vidsnap.Utility.UtilityInterface.ResponseCallBack;

public class Instagram extends Extractor {
    String TAG = Statics.TAG + ":Instagram";

    MainActivity activity;

    String cacheFileName;
    String fileURL;
    String fileSize;
    String videoName;
    Bitmap thumbNail;
    String httpURL;
    String user_cookies;
    String own_cookie;

    boolean own_used;

    Formats formats;


    AnalyzeCallback analyzeCallback;

    public Instagram(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void Analyze(String url, AnalyzeCallback callback) {
        httpURL = url;
        formats = new Formats();
        activity.dialog.show("Downloading Info");
        user_cookies = activity.getStringValue(R.string.key_instagram, null);

        this.analyzeCallback = callback;

        FirebaseManager.instance.getInstaCookie(new CookiesInterface() {
            @Override
            public void onReceivedCookies(String cookies) {
                own_cookie = cookies;
                new HttpRequest(activity, url, null, null, null, null, null, new ResponseCallBack() {
                    @Override
                    public void onReceive(String response) {
                        activity.dialog.show("Analysing");
                        ExtractInfoShared(response);

                    }
                }).start();
            }
        });


    }


    private void ExtractInfoShared(String page) {

        String jsonString;
        if(page==null) {
            tryWithCookies();
            return;
        }
        try {
            Pattern pattern = Pattern.compile("window\\._sharedData\\s*=\\s*(\\{.+?\\});");
            Matcher matcher = pattern.matcher(page);
            if (matcher.find()) {
                jsonString = matcher.group(1);
            } else {
                tryWithCookies();
                return;
            }
            JSONObject media;
            JSONObject jsonObject = new JSONObject(String.valueOf(jsonString));
            JSONArray postPage = UtilityClass.JSONGetter.getArray_or_Null(
                    UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "entry_data"),
                    "PostPage");

            if (postPage != null) {
                JSONObject zero = UtilityClass.JSONGetter.getObj_or_Null(postPage, 0);
                JSONObject graphql = UtilityClass.JSONGetter.getObj_or_Null(zero, "graphql");

                if (graphql != null)
                    media = UtilityClass.JSONGetter.getObj_or_Null(graphql, "shortcode_media");
                else media = UtilityClass.JSONGetter.getObj_or_Null(zero, "media");
                if (media == null) {
                    activity.dialog.show("Attempting different URL");
                    ExtractInfoAdd(page);
                    return;
                }
                setInfo(media);
            } else {
                ExtractInfoAdd(page);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            activity.error("Internal Error Occurred Please try again.",e);
        }

    }

    void RequestWithCookies(String cookies) {
        if (cookies == null || cookies.isEmpty()) {
            activity.dialog.dismiss();
            activity.signInNeeded("Instagram.com says you to login. To download it you need to login Instagram.com",
                    "https://www.instagram.com/accounts/login/",
                    new String[]{"https://www.instagram.com/"},
                    new LoginIdentifier() {
                        @Override
                        public void loggedIn(String cookies) {
                            activity.dialog.show("Adding Cookies");
                            Instagram.this.user_cookies = cookies;
                            activity.setStringValue(R.string.key_instagram, cookies);
                            RequestWithCookies(cookies);
                        }
                    });
            return;
        }
        new HttpRequest(activity, httpURL, cookies, null, null, null, null, new ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                ExtractInfoShared(response);
            }
        }).start();
    }

    void setInfo(JSONObject media) {
        try {

            videoName = getString_or_Null(media, "title");
            //media.getJSONObject("edge_media_to_caption").getJSONArray("edges").getJSONObject(0).getJSONObject("node").getString("text")

            if (videoName == null || videoName.equals("null") || videoName.isEmpty())
                videoName = getString_or_Null(
                        UtilityClass.JSONGetter.getObj_or_Null(
                                getObj_or_Null(
                                        getArray_or_Null(
                                                UtilityClass.JSONGetter.getObj_or_Null(
                                                        media, "edge_media_to_caption")
                                                , "edges")
                                        , 0), "node")
                        , "text");

            if (videoName == null || videoName.equals("null") || videoName.isEmpty())
                videoName = "instagram_video";
            fileURL = getString_or_Null(media, "video_url");
            if (fileURL == null) {
                JSONArray edges = getArray_or_Null(UtilityClass.JSONGetter.getObj_or_Null(media, "edge_sidecar_to_children"), "edges");
                if (edges == null) {
                    activity.error("This media can't be downloaded",new Exception("IDK try it" + media));
                    return;
                }
                for (int i = 0; i < edges.length(); i++) {
                    JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                    if (node.getBoolean("is_video")) {
                        formats.thumbNailsURL.add(nodeToThumb(node));
                        formats.videoURLs.add(nodeToVideo(node));
                    }
                }
            } else{
                formats.videoURLs.add(fileURL);
                formats.thumbNailsURL.add(media.getString("thumbnail_src"));
            }
            videoName = videoName.replaceAll("\n", "");
            videoName = videoName.replaceAll("\\.", "");


            formats.title = videoName;

            updateVideoSize();
        } catch (JSONException e) {
            activity.error("Internal Error Occurred",e);
            e.printStackTrace();
        }

        Log.d(TAG, "ExtractInfo: " + fileURL);


    }

    private String nodeToThumb(JSONObject node) throws JSONException {
        return node.getString("display_url");
    }

    private String nodeToVideo(JSONObject node) throws JSONException {
        return node.getString("video_url");
    }

    int got = 0;

    //short media code => edge_sidecar_to_children => edges[] => o->n {}
    void updateVideoSize() {
        activity.dialog.show("Almost done!!");
        if (formats.qualities.size() < formats.videoURLs.size()) {
            new MiniExecute(activity, formats.videoURLs.get(got), true, 0, new MiniExecutorCallBack() {

                @Override
                public void onBitmapReceive(Bitmap image) {

                }

                @Override
                public void onSizeReceived(int size, int isLast) {
                    formats.raw_quality_size.add(String.valueOf(size));

                    DecimalFormat decimalFormat = new DecimalFormat("#.##");

                    formats.mimeTypes_video.add(MIMEType.VIDEO_MP4);
                    formats.qualities.add("--");
                    formats.quality_size.add(decimalFormat.format(size / Math.pow(10, 6)));
                    got++;
                    updateVideoSize();

                }
            }).start();
            return;
        }
        got=0;
        updateThumbNail();


    }

    void updateThumbNail(){
        if(formats.thumbNailsBitMap.size()<formats.thumbNailsURL.size()){
            new MiniExecute(activity, formats.thumbNailsURL.get(got), false, 0, new MiniExecutorCallBack() {
                @Override
                public void onBitmapReceive(Bitmap image) {
                    got++;
                    formats.thumbNailsBitMap.add(image);
                    updateThumbNail();
                }

                @Override
                public void onSizeReceived(int size, int isLast) {

                }
            }).start();
            return;
        }
        formats.src="Instagram";
        analyzeCallback.onAnalyzeCompleted(formats, true);
    }




    void ExtractInfoAdd(String page) {
        String jsonString = "";
        try {
            Pattern pattern = Pattern.compile("window\\.__additionalDataLoaded\\s*\\(\\s*[^,]+,\\s*(\\{.+?\\})\\s*\\)\\s*;");
            Matcher matcher = pattern.matcher(page);
            if (matcher.find()) {

                jsonString = matcher.group(1);
            }
            if (jsonString == null || jsonString.isEmpty()) {
                tryWithCookies();
                return;
            }
            JSONObject media;
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONObject graphql = UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "graphql");

            media = UtilityClass.JSONGetter.getObj_or_Null(graphql, "shortcode_media");
            if (media == null) {
                activity.error("Something went wrong",new Exception("doesn't find media"));
                return;
            }
            setInfo(media);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void tryWithCookies() {
        if(!own_used && !own_cookie.isEmpty()) {
            RequestWithCookies(own_cookie);
            own_used=true;
        }else {
            RequestWithCookies(user_cookies);
        }
    }

}