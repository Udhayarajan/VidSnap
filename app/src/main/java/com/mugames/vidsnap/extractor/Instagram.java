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

import com.mugames.vidsnap.firebase.FirebaseManager;
import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getArray_or_Null;
import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getObj_or_Null;
import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getString_or_Null;
import static com.mugames.vidsnap.utility.UtilityInterface.*;

import android.util.Log;

public class Instagram extends Extractor {
    String TAG = Statics.TAG + ":Instagram";


    String httpURL;
    String own_cookie;

    boolean own_used;


    public Instagram() {
        super("Instagram");
    }

    @Override
    public void analyze(String url) {
        httpURL = url;
        getDialogueInterface().show("Downloading Info");

        FirebaseManager.getInstance(getContext()).getInstaCookie(new CookiesInterface() {
            @Override
            public void onReceivedCookies(String cookies) {
                own_cookie = cookies;
                HttpRequest request = new HttpRequest(url, response -> {
                    getDialogueInterface().show("Analysing");
                    if (response.getException() != null){
                        getDialogueInterface().error(response.getResponse(),response.getException());
                        return;
                    }
                    try {
                        extractInfoShared(response.getResponse());
                    } catch (JSONException e) {
                        getDialogueInterface().error("Internal Error", e);
                    }
                });
                request.setType(HttpRequest.GET);
                request.start();
            }
        });


    }


    private void extractInfoShared(String page) throws JSONException {
        String jsonString;
        if (page == null) {
            tryWithCookies();
            return;
        }
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
                getDialogueInterface().show("Attempting different URL");
                extractInfoAdd(page);
                return;
            }
            setInfo(media);
        } else {
            extractInfoAdd(page);
        }

    }

    void requestWithCookies(String cookies) {
        if (cookies == null || cookies.isEmpty()) {
            trySignIn("Instagram.com says you to login. To download it you need to login Instagram.com",
                    "https://www.instagram.com/accounts/login/",
                    new String[]{"https://www.instagram.com/"},
                    cookies1 -> {
                        getDialogueInterface().show("Adding Cookies");
                        requestWithCookies(cookies1);
                    });
            return;
        }
        HttpRequest request = new HttpRequest(httpURL, response -> {
            if (response.getException() != null){
                getDialogueInterface().error(response.getResponse(),response.getException());
                return;
            }
            try {
                extractInfoShared(response.getResponse());
            } catch (JSONException e) {
                getDialogueInterface().error("Internal Error", e);
            }
        });
        request.setCookies(cookies);
        request.setType(HttpRequest.GET);
        request.start();
    }

    void setInfo(JSONObject media) throws JSONException {

        String videoName = getString_or_Null(media, "title");
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
        String fileURL = getString_or_Null(media, "video_url");
        if (fileURL == null) {
            JSONArray edges = getArray_or_Null(UtilityClass.JSONGetter.getObj_or_Null(media, "edge_sidecar_to_children"), "edges");
            if (edges == null) {
                getDialogueInterface().error("This media can't be downloaded", new Exception("IDK try it" + media));
                return;
            }
            for (int i = 0; i < edges.length(); i++) {
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                if (node.getBoolean("is_video")) {
                    formats.thumbNailsURL.add(nodeToThumb(node));
                    formats.mainFileURLs.add(nodeToVideo(node));
                    formats.qualities.add("--");
                    formats.fileMime.add(MIMEType.VIDEO_MP4);
                }
            }
        } else {
            formats.mainFileURLs.add(fileURL);
            formats.fileMime.add(MIMEType.VIDEO_MP4);
            formats.qualities.add("--");
            formats.thumbNailsURL.add(media.getString("thumbnail_src"));
        }
        videoName = videoName.replaceAll("\n", "");
        videoName = videoName.replaceAll("\\.", "");


        formats.title = videoName;

        updateVideoSize();

    }

    private String nodeToThumb(JSONObject node) throws JSONException {
        return node.getString("display_url");
    }

    private String nodeToVideo(JSONObject node) throws JSONException {
        return node.getString("video_url");
    }


    //short media code => edge_sidecar_to_children => edges[] => o->n {}
    void updateVideoSize() {
        getDialogueInterface().show("Almost done!!");
        fetchDataFromURLs();
    }


    void extractInfoAdd(String page) throws JSONException {
        String jsonString = "";
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
        if (graphql != null) {
            //Old instagram json response has media
            //But new one has items
            media = UtilityClass.JSONGetter.getObj_or_Null(graphql, "shortcode_media");
            if (media == null) {
                getDialogueInterface().error("Something went wrong", new Exception("doesn't find media"));
                return;
            }
            setInfo(media);
        } else {
            // With new instagram response adds more video quality option
            //This else part helps to extract video from new response
            JSONArray items = jsonObject.getJSONArray("items");
            extractFromItems(items);
        }

    }

    private void extractFromItems(JSONArray items) throws JSONException {
        for (int i = 0; i < items.length(); i++) {
            JSONArray videoVersion = items.getJSONObject(i).getJSONArray("video_versions");
            for (int j = 0; j < videoVersion.length(); j++) {
                JSONObject video = videoVersion.getJSONObject(j);
                formats.mainFileURLs.add(video.getString("url"));
                formats.qualities.add(video.getString("width") + "x" + video.getString("height"));
                formats.fileMime.add(MIMEType.VIDEO_MP4);
            }

            JSONObject imageVersion2 = items.getJSONObject(i).getJSONObject("image_versions2");
            JSONArray candidates = imageVersion2.getJSONArray("candidates");
            formats.thumbNailsURL.add(candidates.getJSONObject(0).getString("url"));
            JSONObject caption = getObj_or_Null(items.getJSONObject(i),"caption");
            String title;
            if (caption!=null){
                title = caption.getString("text");
            }else{
                try{
                    title = items.getJSONObject(i).getString("caption");
                    if (title.equals("null")) title = "Instagram_Reels";
                }catch (JSONException e){
                    title = "Instagram reels";
                }
            }
            formats.title = title;
        }
        updateVideoSize();
    }

    private void tryWithCookies() {
        if (!own_used && !own_cookie.isEmpty()) {
            requestWithCookies(own_cookie);
            own_used = true;
        } else {
            requestWithCookies(getUserCookies());
        }
    }

}