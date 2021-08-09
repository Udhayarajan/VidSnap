package com.mugames.vidsnap.Extractor;

import android.graphics.Bitmap;
import android.util.Log;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.Extractor;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.Utility.UtilityInterface.AnalyzeCallback;
import com.mugames.vidsnap.m3u8;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Twitter extends Extractor {
    String TAG = Statics.TAG + ":Twitter";

    MainActivity activity;

    static String base_url = "https://api.twitter.com/1.1/";
    static String query = "?cards_platform=Web-12&include_cards=1&include_reply_count=1&include_user_entities=0&tweet_mode=extended";

    String httpURL;
    String tweetID;

    //download details
    String selectedURL;
    String fileSize;
    Bitmap thumbNail;
    String cacheFileName;

    String auth = "Bearer AAAAAAAAAAAAAAAAAAAAAPYXBAAAAAAACLXUNDekMxqa8h%2F40K4moUkGsoc%3DTYfbDKbT3jJPCEVnMYqilB28NHfOPqkca3qaAxGfsyKCs0wRbw";
    String videoName;
    String thumbNailURL;


    JSONObject info;

    Formats formats;
    Hashtable<String, String> headers = new Hashtable<>();

    AnalyzeCallback analyzeCallback;

    public Twitter(MainActivity activity) {
        this.activity = activity;
        headers.put("Authorization", auth);
    }

    @Override
    public void Analyze(String url, AnalyzeCallback callback) {
        httpURL = url;
        Reset();
        analyzeCallback = callback;
        tweetID = GetTweetID(url);
        if (tweetID == null) {
            activity.error("Sorry!! Tweet doesn't exist",null);
            return;
        }
        GetToken();

    }

    private void Reset() {
        formats = new Formats();
    }

    String GetTweetID(String url) {
        Pattern pattern = Pattern.compile("(?:(?:i/web|[^/]+)/status|statuses)/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        pattern = Pattern.compile("i/broadcasts/([0-9a-zA-Z]{13})");
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    private void GetToken() {
        activity.dialog.show("Getting Token");
        new HttpRequest(activity, base_url + "guest/activate.json", null,
                headers, HttpRequest.POST, null, null, new UtilityInterface.ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                try {
                    Log.d(TAG, "onReceive: " + new JSONObject(response).getString("guest_token"));
                    headers.put("x-guest-token", new JSONObject(response).getString("guest_token"));
                    if (httpURL.contains("status")) ExtractVideo();
                    if (httpURL.contains("broadcasts")) ExtractBroadcasts();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void ExtractVideo() {
        new HttpRequest(activity, base_url + "statuses/show/" + tweetID + ".json" + query, null, headers,
                null, null, null, new UtilityInterface.ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                try {
                    activity.dialog.show("Identifying..");
                    JSONObject jsonObject = new JSONObject(response);
                    formats.title = jsonObject.getString("full_text").split("[→]")[0];

                    JSONObject extended_entities = UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "extended_entities");
                    if (extended_entities != null) {
                        JSONObject media = extended_entities.getJSONArray("media").getJSONObject(0);
                        FromVideoInfo(media);
                        UpdateUI();
                    } else {
                        JSONObject card = UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "card");
                        if (card != null) {
                            JSONObject binding_values = card.getJSONObject("binding_values");
                            String[] strings = card.getString("name").split(":");
                            String cardName = strings[strings.length - 1];
                            if (cardName.equals("player")) {
                                new Twitch(activity).ExtractURLFromClips(get_binding_value(binding_values, "player_url"), new Twitch.TwitchInfoInterface() {
                                    @Override
                                    public void onDone(Formats format) {
                                        formats = format;
                                        UpdateUI();
                                    }
                                });
                            } else if (cardName.equals("periscope_broadcast")) {
                                String s = get_binding_value(binding_values, "url");
                                // videoInfo.setVideoURL(s);
                                if (s == null || s.isEmpty())
                                    s = get_binding_value(binding_values, "player_url");
                                //Creates new Instance for periscope
                                new Periscope(activity).ExtractInfo(s, analyzeCallback);
                            } else if (cardName.equals("broadcast")) {
                                tweetID = GetTweetID(get_binding_value(binding_values, "broadcast_url"));
                                ExtractBroadcasts();
                            }
//                            else if(cardName.equals("summary")) videoInfo.setVideoURL(get_binding_value(binding_values,"card_url"));
                            else if (cardName.equals("unified_card")) {
                                JSONObject unified_card = new JSONObject(get_binding_value(binding_values, "unified_card"));
                                JSONObject media_entities = unified_card.getJSONObject("media_entities");
                                JSONObject media = media_entities.getJSONObject(media_entities.names().getString(0));
                                FromVideoInfo(media);
                                UpdateUI();
                            } else {
                                boolean isAmplify = cardName.equals("amplify");
                                String vmap_url;
                                if (isAmplify)
                                    vmap_url = get_binding_value(binding_values, "amplify_url_vmap");
                                else
                                    vmap_url = get_binding_value(binding_values, "player_stream_url");
                                for (String s : new String[]{"_original", "_x_large", "_large", "", "_small"}) {
                                    JSONObject image = new JSONObject(get_binding_value(binding_values, "player_image" + s));
                                    String img_url = UtilityClass.JSONGetter.getString_or_Null(image, "url");
                                    if (img_url != null && !img_url.contains("/player-placeholder")) {
                                        formats.thumbNailURL = img_url;
                                        break;
                                    }
                                }
                                FromVMap(vmap_url);
                            }
                        } else {
                            activity.error("This media can't be downloaded. It may be a retweet so paste URL of main tweet",null);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void FromVMap(String url) {
        new HttpRequest(activity, url, null, null,
                null, null, null, new UtilityInterface.ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                Pattern pattern = Pattern.compile("(?<=<tw:videoVariants>)[\\s\\S]*(?=</tw:videoVariants>)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    response = matcher.group(0);
                    response = response.trim();
                    for (String s : response.split("\n")) {
                        pattern = Pattern.compile("(?<=url=\")(.*?)\".*(?<=content_type=\")(.*?)\"");
                        matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            if (matcher.group(2).equals(MIMEType.VIDEO_MP4)) {
                                String u = UtilityClass.decodeHTML(matcher.group(1));
                                formats.videoURLs.add(u);
                                formats.qualities.add(Resolution(u));
                            }
                        }
                    }
                    UpdateUI();
                    return;
                }
                activity.error("Sorry! Something wrong in vmap.",new Exception("Problem with vmap"));
            }
        }).start();
    }

    String get_binding_value(JSONObject binding_value, String filed) {
        try {
            JSONObject jsonObject = binding_value.getJSONObject(filed);
            return jsonObject.getString(jsonObject.getString("type").toLowerCase() + "_value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void FromVideoInfo(JSONObject media) {
        JSONObject video_info;
        try {
            video_info = media.getJSONObject("video_info");
            JSONArray variants = video_info.getJSONArray("variants");
            for (int i = 0; i < variants.length(); i++) {
                JSONObject data = variants.getJSONObject(i);
                if (data.getString("content_type").equals(MIMEType.VIDEO_MP4)) {
                    String s = data.getString("url");
                    formats.qualities.add(Resolution(s));
                    formats.videoURLs.add(s);
                }
            }
            thumbNailURL = media.getString("media_url");
            if (thumbNailURL.isEmpty()) thumbNailURL = media.getString("media_url_https");
            if (thumbNailURL.startsWith("http:/"))
                thumbNailURL = thumbNailURL.replaceAll("http:/", "https:/");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    int uiIndex = 0;

    void UpdateUI() {
        videoName = formats.title;
        activity.dialog.show("Setting up...");
        new MiniExecute(activity, formats.videoURLs.get(uiIndex), true, 0, new UtilityInterface.MiniExecutorCallBack() {

            @Override
            public void onBitmapReceive(Bitmap image) {

            }

            @Override
            public void onSizeReceived(int size, int isLast) {
                formats.raw_quality_size.add(String.valueOf(size));
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                formats.quality_size.add(decimalFormat.format(size / Math.pow(10, 6)));
                uiIndex++;

                if (formats.quality_size.size() == formats.videoURLs.size()) {
                    uiIndex = 0;
                    if (thumbNailURL == null) thumbNailURL = formats.thumbNailURL;
                    new MiniExecute(activity, thumbNailURL, false, 0, new UtilityInterface.MiniExecutorCallBack() {
                        @Override
                        public void onBitmapReceive(Bitmap image) {
                            SetUp(image);
                        }

                        @Override
                        public void onSizeReceived(int size, int isLast) {

                        }
                    }).start();
                } else
                    UpdateUI();
            }
        }).start();

        activity.dialog.show("Almost done!!");


    }

    void SetUp(Bitmap thumbNail) {

        formats.thumbNailBit = thumbNail;
        formats.src="Twitter";
        analyzeCallback.onAnalyzeCompleted(formats, true);
    }


    String Resolution(String url) {
        Pattern pattern = Pattern.compile("/(\\d+x\\d+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find())
            return matcher.group(1);
        return "--";
    }

    void ExtractBroadcasts() {
        new HttpRequest(activity, String.format(base_url + "broadcasts/show.json?ids=%s", tweetID), null, headers,
                null, null, null, new UtilityInterface.ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                try {
                    JSONObject broadcasts = new JSONObject(response).getJSONObject("broadcasts").getJSONObject(tweetID);
                    info = ExtractInfo(broadcasts);
                    String mediaKey = broadcasts.getString("media_key");
                    new HttpRequest(activity, String.format(base_url + "live_video_stream/status/%s", mediaKey),
                            null, headers, null, null, null, new UtilityInterface.ResponseCallBack() {
                        @Override
                        public void onReceive(String response) {
                            Pattern pattern = Pattern.compile("\\{[\\s\\S]+\\}");
                            Matcher matcher = pattern.matcher(response);
                            matcher.find();
                            response = matcher.group(0);
                            try {
                                JSONObject source = new JSONObject(response).getJSONObject("source");
                                String m3u8_url = UtilityClass.JSONGetter.getString_or_Null(source, "noRedirectPlaybackUrl");
                                if (nullOrEmpty(m3u8_url)) m3u8_url = source.getString("location");
                                if (m3u8_url.contains("/live_video_stream/geoblocked/")) {
                                    activity.error("Geo restricted try with VPN",null);
                                    return;
                                }
                                new m3u8(activity).Extract_m3u8(m3u8_url, info, analyzeCallback);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    JSONObject ExtractInfo(JSONObject broadcast) {
        try {
            String title = UtilityClass.JSONGetter.getString_or_Null(broadcast, "status");
            if (title == null) title = "Periscope Broadcast";
            String uploader = UtilityClass.JSONGetter.getString_or_Null(broadcast, "user_display_name");
            if (uploader == null) uploader = broadcast.getString("username");
            title = String.format("%s - %s", uploader, title);
            String thumbnail = null;
            for (String img : new String[]{"image_url_medium", "image_url_small", "image_url"}) {
                thumbnail = broadcast.getString(img);
                if (!nullOrEmpty(thumbnail)) break;
            }
            String resolution = broadcast.get("width") + "x" + broadcast.get("height");
            boolean isLive = !"ENDED".equals(broadcast.getString("state"));
            String js = String.format("{\"title\":\"%s\",\"thumbNailURL\":\"%s\",\"isLive\":\"%s\",\"resolution\":\"%s\"}", title, thumbnail, isLive, resolution);
            return new JSONObject(js);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean nullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }


}