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

import com.mugames.json.XML;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.decodeHTML;
import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getArray_or_Null;
import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getObj_or_Null;
import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.getString_or_Null;

@Deprecated
public class Facebook extends Extractor {
    static final int SUCCESS = -1;//Null if fails
    String TAG = Statics.TAG + ":Facebook";

    String ID;
    String url;


    boolean tried_with_cookies;
    boolean tried_with_forceEng;


    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.122 Safari/537.36";

    static String PAGELET_REGEX = "(?:pagelet_group_mall|permalink_video_pagelet|hyperfeed_story_id_[0-9a-f]+)";


    Hashtable<String, String> headers;


    public Facebook() {
        super("FaceBook");
    }


    String getId(String url) {
        Pattern pattern = Pattern.compile("(?:https?://(?:[\\w-]+\\.)?(?:facebook\\.com|facebookcorewwwi\\.onion)/(?:[^#]*?#!/)?(?:(?:video/video\\.php|photo\\.php|video\\.php|video/embed|story\\.php|watch(?:/live)?/?)\\?(?:.*?)(?:v|video_id|story_fbid)=|[^/]+/videos/(?:[^/]+/)?|[^/]+/posts/|groups/[^/]+/permalink/|watchparty/)|facebook:)([0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    @Override
    public void analyze(String url) {
        ID = getId(url);
        headers = new Hashtable<>();

        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("User-Agent", userAgent);

        if (url.startsWith("facebook:"))
            url = String.format("https://www.facebook.com/video/video.php?v=%s", ID);
        this.url = url;
        getDialogueInterface().show("Accessing Server...");
        extractInfo();
    }

    private void extractInfo() {
        url = url.replaceAll("://m.facebook\\.com/", "://www.facebook.com/");
        HttpRequest request = new HttpRequest(url, response -> {
            if (response.getException() != null) {
                getDialogueInterface().error(response.getResponse(), response.getException());
                return;
            }
            scratchWebPage(response.getResponse());
        });
        request.setType(HttpRequest.GET);
        request.setHeaders(headers);
        request.start();
    }

    void extractForceEng() {
        url = url.replaceAll("://m.facebook\\.com/", "://www.facebook.com/");
        url = url.replaceAll("://www.facebook\\.com/", "://en-gb.facebook.com/");
        headers.put("Accept-Language", "en-GB,en-US,en");
        HttpRequest request = new HttpRequest(url, response -> {
            tried_with_forceEng = true;
            if (response.getException() != null) {
                getDialogueInterface().error(response.getResponse(), response.getException());
                return;
            }
            scratchWebPage(response.getResponse());
        });
        request.setType(HttpRequest.GET);
        request.setHeaders(headers);
        request.start();

    }

    private void scratchWebPage(String webPage) {
        getDialogueInterface().show("Analyzing..");
        try {
            String server_js_data = null;
            Matcher matcher = Pattern.compile("handleServerJS\\((\\{.+\\})(?:\\);|,\")").matcher(webPage);
            if (matcher.find()) server_js_data = matcher.group(1);
            else {
                matcher = Pattern.compile("\\bs\\.handle\\((\\{.+?\\})\\);").matcher(webPage);
                if (matcher.find()) server_js_data = matcher.group(1);
            }

            Object video_data = null;

            if (isNonNullOrEmpty(server_js_data)) {
                video_data = grab_video_data(new JSONObject(server_js_data).getJSONArray("instances"));
            }

            if (video_data == null) {
                matcher = Pattern.compile("bigPipe\\.onPageletArrive\\((\\{.+?\\})\\)\\s*;\\s*\\}\\s*\\)\\s*,\\s*[\"']onPageletArrive\\s+" + PAGELET_REGEX).matcher(webPage);
                if (matcher.find()) server_js_data = matcher.group(1);
                else {
                    matcher = Pattern.compile(String.format("bigPipe\\.onPageletArrive\\((\\{.*?id\\s*:\\s*\\\"%s\\\".*?\\})\\);", PAGELET_REGEX)).matcher(webPage);
                    if (matcher.find()) server_js_data = matcher.group(1);
                }
                if (isNonNullOrEmpty(server_js_data))
                    video_data = grabFromJSmodsInstance(new JSONObject(server_js_data));
            }

            if (video_data == null) {
                video_data = grabRelayPrefetchedDataSearchUrl(webPage);
            }

            if (video_data == null) {
                matcher = Pattern.compile("class=\"[^\"]*uiInterstitialContent[^\"]*\"><div>(.*?)</div>").matcher(webPage);
                if (matcher.find()) {
                    getDialogueInterface().error("This video unavailable. FB says : " + matcher.group(1), null);
                    return;
                }
                if (webPage.contains("You must log in to continue"))
                    if (!tried_with_cookies) {
                        tryWithCookies();
                        return;
                    }
            }

            if (video_data == null) {
                if (!tried_with_forceEng)
                    extractForceEng();
                else getDialogueInterface().error("This video can't be Downloaded", null);
            } else {
                Matcher m;
                if (formats.thumbNailsURL.isEmpty()) {
                    m = Pattern.compile("\"thumbnailImage\":\\{\"uri\":\"(.*?)\"\\}").matcher(webPage);
                    if (m.find()) formats.thumbNailsURL.add(m.group(1));
                    else {
                        m = Pattern.compile("\"thumbnailUrl\":\"(.*?)\"").matcher(webPage);
                        if (m.find()) formats.thumbNailsURL.add(m.group(1));
                    }
                }
                if (formats.title == null || formats.title.equals("null")) {
                    m = Pattern.compile("(?:true|false),\"name\":\"(.*?)\",\"savable").matcher(webPage);
                    if (m.find()) formats.title = m.group(1);
                    else {
                        m = Pattern.compile("<[Tt]itle id=\"pageTitle\">(.*?) \\| Facebook<\\/title>").matcher(webPage);
                        if (m.find()) formats.title = decodeHTML(m.group(1));
                        else {
                            m = Pattern.compile("title\" content=\"(.*?)\"").matcher(webPage);
                            if (m.find()) formats.title = decodeHTML(m.group(1));
                        }

                    }
                    if (formats.title == null || formats.title.equals("null"))
                        formats.title = "Facebook_Video";
                }
                UpdateUI();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            getDialogueInterface().error("Something went wrong", e);
        }
    }


    private void UpdateUI() {
        fetchDataFromURLs();
    }


    void tryWithCookies() {
        if (getUserCookies() == null) {
            trySignIn("Facebook requested you to sign-in. Without sign-in video can't be downloaded",
                    "https://www.facebook.com/login/",
                    new String[]{"https://m.facebook.com/login/save-device/?login_source=login#_=_", "https://m.facebook.com/?_rdr",
                            "https://m.facebook.com/home.php?_rdr", "https://m.facebook.com/home.php",
                            "https://m.facebook.com/?ref=dbl&_rdr",
                            "https://m.facebook.com/?ref=dbl&_rdr#~!/home.php?ref=dbl",
                            "https://m.facebook.com/?ref=dbl&_rdr#!/home.php?ref=dbl",
                    },
                    cookies -> {
                        tryWithCookies();
                    });
            return;
        }

        tried_with_cookies = true;
        getDialogueInterface().show("Adding cookies...");
        HttpRequest request = new HttpRequest(url, response -> {
            if (response.getException() != null) {
                getDialogueInterface().error(response.getResponse(), response.getException());
                return;
            }
            scratchWebPage(response.getResponse());
        });
        request.setCookies(getUserCookies());
        request.setType(HttpRequest.GET);
        request.setHeaders(headers);
        request.start();
    }

    Object grabRelayPrefetchedDataSearchUrl(String webpage) throws JSONException {
        JSONObject data = grabRelayPrefetchedData(webpage, new String[]{"\"dash_manifest\"", "\"playable_url\""});
        if (data != null) {
            JSONArray nodes = null;
            try {
                nodes = data.getJSONArray("nodes");
            } catch (JSONException e) {
            }
            JSONObject node = UtilityClass.JSONGetter.getObj_or_Null(data, "node");
            if (nodes == null && node != null) {
                nodes = new JSONArray();
                nodes.put(data);
            }

            if (nodes != null) {
                for (int j = 0; j < nodes.length(); j++) {
                    node = nodes.getJSONObject(j).getJSONObject("node");
                    JSONArray attachments = null;
                    JSONObject story = node.getJSONObject("comet_sections").getJSONObject("content").getJSONObject("story");
                    try {
                        attachments = story.getJSONObject("attached_story").getJSONArray("attachments");
                    } catch (JSONException e) {
                        attachments = story.getJSONArray("attachments");
                    }

                    for (int k = 0; k < attachments.length(); k++) {
                        //attachments.getJSONObject(k).getJSONObject("style_type_renderer").getJSONObject("attachment");
                        JSONObject attachment = UtilityClass.JSONGetter.getObj_or_Null(UtilityClass.JSONGetter.getObj_or_Null(getObj_or_Null(attachments, k), "style_type_renderer"), "attachment");
                        JSONArray ns = getArray_or_Null(UtilityClass.JSONGetter.getObj_or_Null(attachment, "all_subattachments"), "nodes");
                        if (ns != null) {
                            for (int l = 0; l < ns.length(); l++) {
                                parseAttachment(ns.getJSONObject(l), "media");
                            }
                        }
                        parseAttachment(attachment, "media");
                    }

                }
            }

            JSONArray edges = getArray_or_Null(UtilityClass.JSONGetter.getObj_or_Null(UtilityClass.JSONGetter.getObj_or_Null(data, "mediaset"), "currMedia"), "edges");
            if (edges != null) {
                for (int j = 0; j < edges.length(); j++) {
                    JSONObject edge = edges.getJSONObject(j);
                    parseAttachment(edge, "node");
                }
            }

            JSONObject video = UtilityClass.JSONGetter.getObj_or_Null(data, "video");
            if (video != null) {
                JSONArray attachments = null;
                try {
                    attachments = video.getJSONObject("story").getJSONArray("attachments");
                } catch (JSONException e) {
                    attachments = getArray_or_Null(UtilityClass.JSONGetter.getObj_or_Null(video, "creation_story"), "attachments");
                }
                if (attachments != null) {
                    for (int j = 0; j < attachments.length(); j++) {
                        parseAttachment(attachments.getJSONObject(j), "media");
                    }
                }
                if (formats.mainFileURLs.isEmpty()) parse_graphql_video(video);
            }

            if (!formats.mainFileURLs.isEmpty()) return SUCCESS;
        }

        return null;
    }

    String grabRelayData(String webPage, String[] searchWords) {
        Matcher m = Pattern.compile("handleWithCustomApplyEach\\(.*?,(.*)\\);").matcher(webPage);
        while (m.find()) {
            Matcher m1 = Pattern.compile("(\\{.*[^);]\\})\\);").matcher(Objects.requireNonNull(m.group(1)));
            if (m1.find())
                for (String s : searchWords)
                    if (m1.group(1).contains(s))
                        return m1.group(1);
        }
        return null;
    }

    JSONObject grabRelayPrefetchedData(String webPage, String[] filter) {
        String jsonString = grabRelayData(webPage, filter);
        if (isNonNullOrEmpty(jsonString)) {
            try {
                JSONObject jsonObj = new JSONObject(jsonString);
                JSONArray require = jsonObj.getJSONArray("require");
                for (int i = 0; i < require.length(); i++) {
                    JSONArray array = require.getJSONArray(i);
                    if (array.getString(0).equals("RelayPrefetchedStreamCache"))
                        return array.getJSONArray(3).getJSONObject(1).getJSONObject("__bbox").getJSONObject("result").getJSONObject("data");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    void parseAttachment(JSONObject attachment, String key) throws JSONException {
        JSONObject media = UtilityClass.JSONGetter.getObj_or_Null(attachment, key);
        if (media != null && media.getString("__typename").equals("Video")) {
            parse_graphql_video(media);
        }

    }

    void parse_graphql_video(JSONObject media) throws JSONException {
        String res;
        try {
            formats.thumbNailsURL.add(media.getJSONObject("thumbnailImage").getString("uri"));
        } catch (JSONException e) {
            formats.thumbNailsURL.add(media.getJSONObject("preferred_thumbnail").getJSONObject("image").getString("uri"));
        }

        String title = getString_or_Null(media, "name");
        if (title == null)
            title = getString_or_Null(UtilityClass.JSONGetter.getObj_or_Null(media, "savable_description"), "text");
        formats.title = title;

        String dash_xml = getString_or_Null(media, "dash_manifest");
        if (dash_xml != null) {
            extractFromDash(dash_xml);
        }
        try {
            res = media.get("width") + "x" + media.get("height");
        } catch (JSONException e) {
            res = media.get("original_width") + "x" + media.get("original_height");
        }
        for (String suffix : new String[]{"", "_quality_hd"}) {
            String playable_url = getString_or_Null(media, "playable_url" + suffix);

            if (playable_url == null || playable_url.equals("null")) continue;
            formats.mainFileURLs.add(playable_url);
            formats.fileMime.add(MIMEType.VIDEO_MP4);
            formats.audioURLs.add(null);
            formats.audioMime.add(null);

            if (suffix.equals(""))
                formats.qualities.add(res + "(" + "SD" + ")");
            if (suffix.equals("_quality_hd"))
                formats.qualities.add(res + "(" + "HD" + ")");
        }
    }

    void extractFromDash(String xml) {
        xml = xml.replaceAll("x3C", "<");
        xml = xml.replaceAll("\\\\\u003C", "<");

        try {
            JSONArray AdaptionSet = XML.toJSONObject(xml).getJSONObject("MPD").getJSONObject("Period").getJSONArray("AdaptationSet");
            JSONArray videos = AdaptionSet.getJSONObject(0).getJSONArray("Representation");
            JSONObject audios = AdaptionSet.getJSONObject(1);

            String audio_url;
            String audio_mime;
            String res = null;
            String pre = "";
            JSONObject audio_rep;

            try {
                audio_rep = audios.getJSONObject("Representation");
            } catch (JSONException e) {
                audio_rep = audios.getJSONArray("Representation").getJSONObject(0);
            }

            audio_url = audio_rep.getString("BaseURL");
            audio_mime = getString_or_Null(audio_rep, "_mimeType");
            if (audio_mime == null) audio_mime = audio_rep.getString("mimeType");
            else pre = "_";

            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                String video_url = video.getString("BaseURL");
                try {
                    res = video.getString(pre + "FBQualityLabel") + "(" + video.getString(pre + "FBQualityClass").toUpperCase() + ")";
                } catch (JSONException e) {
                    res = video.get(pre + "width") + "x" + video.get(pre + "height");
                }
                String video_mime = video.getString(pre + "mimeType");

                formats.fileMime.add(video_mime);
                formats.mainFileURLs.add(video_url);
                formats.qualities.add(res);
                formats.audioURLs.add(audio_url);
                formats.audioMime.add(audio_mime);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    Object grabFromJSmodsInstance(JSONObject js_data) {
        if (isNonNullOrEmpty(String.valueOf(js_data))) {
            try {
                return grab_video_data(js_data.getJSONObject("jsmods").getJSONArray("instances"));
            } catch (JSONException e) {
            }
        }
        return null;
    }

    Object grab_video_data(JSONArray instance) {
        try {
            for (int i = 0; i < instance.length(); i++) {
                JSONArray item = instance.getJSONArray(i);
                if (item.getJSONArray(1).getString(0).equals("VideoConfig")) {
                    JSONObject video_details = item.getJSONArray(2).getJSONObject(0);
                    JSONObject videoData = video_details.getJSONArray("videoData").getJSONObject(0);

                    String dash_xml = getString_or_Null(videoData, "dash_manifest");
                    if (dash_xml != null) {
                        extractFromDash(dash_xml);
                    }

                    for (String s : new String[]{"hd", "sd"}) {
                        String url = getString_or_Null(videoData, s + "_src");
                        if (url == null || url.equals("null")) continue;
                        formats.mainFileURLs.add(url);
                        formats.qualities.add(videoData.getString("original_width") + "x" +
                                videoData.getString("original_height") + "(" +
                                s.toUpperCase() + ")"
                        );
                        formats.fileMime.add(MIMEType.AUDIO_MP4);
                        formats.audioURLs.add(null);
                        formats.audioMime.add(null);

                    }

                    return SUCCESS;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    boolean isNonNullOrEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
