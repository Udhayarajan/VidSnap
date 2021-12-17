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

package com.mugames.vidsnap.extractor;

import static com.mugames.vidsnap.utility.MIMEType.VIDEO_MP4;

import android.util.Log;

import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.m3u8;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Twitter extends Extractor {
    String TAG = Statics.TAG + ":Twitter";

    static String base_url = "https://api.twitter.com/1.1/";
    static String query = "?cards_platform=Web-12&include_cards=1&include_reply_count=1&include_user_entities=0&tweet_mode=extended";

    String httpURL;
    String tweetID;


    String auth = "Bearer AAAAAAAAAAAAAAAAAAAAAPYXBAAAAAAACLXUNDekMxqa8h%2F40K4moUkGsoc%3DTYfbDKbT3jJPCEVnMYqilB28NHfOPqkca3qaAxGfsyKCs0wRbw";



    JSONObject info;

    Hashtable<String, String> headers = new Hashtable<>();



    public Twitter() {
        super("Twitter");
        headers.put("Authorization", auth);
    }

    @Override
    public void analyze(String url) {
        httpURL = url;
        tweetID = getTweetID(url);
        if (tweetID == null) {
            getDialogueInterface().error("Sorry!! Tweet doesn't exist",null);
            return;
        }
        getToken();

    }

    String getTweetID(String url) {
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


    private void getToken() {
        getDialogueInterface().show("Getting Token");
        HttpRequest request = new HttpRequest(base_url+"guest/activate.json",getDialogueInterface(),response -> {
            try {
                Log.d(TAG, "onReceive: " + new JSONObject(response.getResponse()).getString("guest_token"));
                headers.put("x-guest-token", new JSONObject(response.getResponse()).getString("guest_token"));
                if (httpURL.contains("status")) extractVideo();
                if (httpURL.contains("broadcasts")) extractBroadcasts();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        request.setType(HttpRequest.POST);
        request.setHeaders(headers);
        request.start();
    }

    void extractVideo() {
        HttpRequest request = new HttpRequest(base_url+"statuses/show/"+tweetID+".json"+query,getDialogueInterface(),response -> identifyDownloader(response.getResponse()));
        request.setType(HttpRequest.GET);
        request.setHeaders(headers);
        request.start();
    }

    private void identifyDownloader(String response){
        try {
            getDialogueInterface().show("Identifying..");
            JSONObject jsonObject = new JSONObject(response);
            formats.title = jsonObject.getString("full_text").split("[→]")[0];

            JSONObject extended_entities = UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "extended_entities");
            if (extended_entities != null) {
                JSONObject media = extended_entities.getJSONArray("media").getJSONObject(0);
                fromVideoInfo(media);
                updateUI();
            } else {
                JSONObject card = UtilityClass.JSONGetter.getObj_or_Null(jsonObject, "card");
                if (card != null) {
                    JSONObject binding_values = card.getJSONObject("binding_values");
                    String[] strings = card.getString("name").split(":");
                    String cardName = strings[strings.length - 1];
                    switch (cardName) {
                        case "player":
                            new Twitch(getDialogueInterface()).extractURLFromClips(get_binding_value(binding_values, "player_url"), this::updateUI);
                            break;
                        case "periscope_broadcast":
                            String s = get_binding_value(binding_values, "url");
                            if (s == null || s.isEmpty())
                                s = get_binding_value(binding_values, "player_url");
                            new Periscope(this).extractInfo(s);
                            break;
                        case "broadcast":
                            tweetID = getTweetID(get_binding_value(binding_values, "broadcast_url"));
                            extractBroadcasts();
                            break;
//                            else if(cardName.equals("summary")) videoInfo.setVideoURL(get_binding_value(binding_values,"card_url"));
                        case "unified_card":
                            JSONObject unified_card = new JSONObject(get_binding_value(binding_values, "unified_card"));
                            JSONObject media_entities = unified_card.getJSONObject("media_entities");
                            JSONObject media = media_entities.getJSONObject(media_entities.names().getString(0));
                            fromVideoInfo(media);
                            updateUI();
                            break;
                        default:
                            boolean isAmplify = cardName.equals("amplify");
                            String vmap_url;
                            if (isAmplify)
                                vmap_url = get_binding_value(binding_values, "amplify_url_vmap");
                            else
                                vmap_url = get_binding_value(binding_values, "player_stream_url");
                            for (String s1 : new String[]{"_original", "_x_large", "_large", "", "_small"}) {
                                JSONObject image = new JSONObject(get_binding_value(binding_values, "player_image" + s1));
                                String img_url = UtilityClass.JSONGetter.getString_or_Null(image, "url");
                                if (img_url != null && !img_url.contains("/player-placeholder")) {
                                    formats.thumbNailsURL.add(img_url);
                                    break;
                                }
                            }
                            fromVMap(vmap_url);
                            break;
                    }
                } else {
                    getDialogueInterface().error("This media can't be downloaded. It may be a retweet so paste URL of main tweet",null);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fromVMap(String url) {

        HttpRequest request = new HttpRequest(url,getDialogueInterface(),res -> {
            String response = res.getResponse();
            Pattern pattern = Pattern.compile("(?<=<tw:videoVariants>)[\\s\\S]*(?=</tw:videoVariants>)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                response = matcher.group(0);
                response = response.trim();
                for (String s : response.split("\n")) {
                    pattern = Pattern.compile("(?<=url=\")(.*?)\".*(?<=content_type=\")(.*?)\"");
                    matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        if (matcher.group(2).equals(VIDEO_MP4)) {
                            String u = UtilityClass.decodeHTML(matcher.group(1));
                            formats.mainFileURLs.add(u);
                            formats.fileMime.add(VIDEO_MP4);
                            formats.qualities.add(resolution(u));
                        }
                    }
                }
                updateUI();
                return;
            }
            getDialogueInterface().error("Sorry! Something wrong in vmap.",new Exception("Problem with vmap"));
        });
        request.setType(HttpRequest.GET);
        request.start();
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

    private void fromVideoInfo(JSONObject media) {
        JSONObject video_info;
        try {
            video_info = media.getJSONObject("video_info");
            JSONArray variants = video_info.getJSONArray("variants");
            for (int i = 0; i < variants.length(); i++) {
                JSONObject data = variants.getJSONObject(i);
                if (data.getString("content_type").equals(VIDEO_MP4)) {
                    String s = data.getString("url");
                    formats.qualities.add(resolution(s));
                    formats.fileMime.add(VIDEO_MP4);
                    formats.mainFileURLs.add(s);
                }
            }
            String thumbNailURL = media.getString("media_url");
            if (thumbNailURL.isEmpty()) thumbNailURL = media.getString("media_url_https");
            if (thumbNailURL.startsWith("http:/"))
                thumbNailURL = thumbNailURL.replaceAll("http:/", "https:/");
            formats.thumbNailsURL.add(thumbNailURL);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    void updateUI() {
        getDialogueInterface().show("Setting up...");
        fetchDataFromURLs();

    }


    String resolution(String url) {
        Pattern pattern = Pattern.compile("/(\\d+x\\d+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find())
            return matcher.group(1);
        return "--";
    }

    void extractBroadcasts() {
        HttpRequest request = new HttpRequest(String.format(base_url + "broadcasts/show.json?ids=%s", tweetID),getDialogueInterface(),response -> parseBroadcastResponse(response.getResponse()));
        request.setHeaders(headers);
        request.setType(HttpRequest.GET);
        request.start();
    }

    void parseBroadcastResponse(String response){
        try {
            JSONObject broadcasts = new JSONObject(response).getJSONObject("broadcasts").getJSONObject(tweetID);
            info = extractInfo(broadcasts);
            String mediaKey = broadcasts.getString("media_key");

            HttpRequest request = new HttpRequest(String.format(base_url + "live_video_stream/status/%s", mediaKey),getDialogueInterface(),res1 -> {
                String response1 = res1.getResponse();
                Pattern pattern = Pattern.compile("\\{[\\s\\S]+\\}");
                Matcher matcher = pattern.matcher(response1);
                matcher.find();
                response1 = matcher.group(0);
                try {
                    JSONObject source = new JSONObject(response1).getJSONObject("source");
                    String m3u8_url = UtilityClass.JSONGetter.getString_or_Null(source, "noRedirectPlaybackUrl");
                    if (nullOrEmpty(m3u8_url)) m3u8_url = source.getString("location");
                    if (m3u8_url.contains("/live_video_stream/geoblocked/")) {
                        getDialogueInterface().error("Geo restricted try with VPN",null);
                        return;
                    }
                    new m3u8(this).extract_m3u8(m3u8_url, info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            request.setHeaders(headers);
            request.setType(HttpRequest.GET);
            request.start();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    JSONObject extractInfo(JSONObject broadcast) {
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