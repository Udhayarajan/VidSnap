package com.mugames.vidsnap.Extractor;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.m3u8;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.Utility.UtilityInterface.AnalyzeCallback;
import static com.mugames.vidsnap.Utility.UtilityInterface.ResponseCallBack;

/**
 * @see Twitter it is only class access this class
 * Pasting link from Periscope won't work but if a tweet contain Periscope it can be downloaded
 * In future it will be extended
 */
public class Periscope {
    MainActivity activity;


    ArrayList<ArrayList<String>> manifest;


    AnalyzeCallback analyzeCallback;
    JSONObject data;


    public Periscope(MainActivity activity) {
        this.activity = activity;
    }

    String GetID(String s) {
        Pattern pattern = Pattern.compile("https?://(?:www\\.)?(?:periscope|pscp)\\.tv/[^/]+/([^/?#]+)");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public void ExtractInfo(String url, AnalyzeCallback analyzeCallback) {
        String id = GetID(url);
        this.analyzeCallback = analyzeCallback;
        activity.dialog.show("Periscope video");

        manifest = new ArrayList<ArrayList<String>>();
        new HttpRequest(activity,
                String.format("https://api.periscope.tv/api/v2/accessVideoPublic?broadcast_id=%s", id),
                null, null, null, null, null, new ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                try {
                    JSONObject stream = new JSONObject(response);
                    JSONObject broadcast = stream.getJSONObject("broadcast");
                    data = ExtractData(broadcast);
                    ArrayList<String> video_urls = new ArrayList<>();
                    for (String format_id : new String[]{"replay", "rtmp", "hls", "https_hls", "lhls", "lhlsweb"}) {
                        String video_url = stream.getString(format_id + "_url");
                        if (nullOrEmpty(video_url) || video_urls.contains(video_url)) continue;
                        video_urls.add(video_url);
                        if (!format_id.equals("rtmp")) {
                            new m3u8(activity).Extract_m3u8(video_url, data, analyzeCallback);
                            break;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private JSONObject ExtractData(JSONObject broadcast) {
        try {
            String title = broadcast.getString("status");
            String thumbnail = null;
            if (nullOrEmpty(title)) title = "Periscope Broadcast";
            String uploader = broadcast.getString("user_display_name");
            if (nullOrEmpty(uploader)) uploader = broadcast.getString("username");
            title = String.format("%s - %s", uploader, title);
            for (String img : new String[]{"image_url_medium", "image_url_small", "image_url"}) {
                thumbnail = broadcast.getString(img);
                if (!nullOrEmpty(thumbnail)) break;
            }
            boolean isLive = !"ENDED".equals(broadcast.getString("state").toLowerCase());
            String resolution = broadcast.getString("width") + "x" + broadcast.getString("height");
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


    public void Download(String fileName, String path) {
        //filled later
    }
}