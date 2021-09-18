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

package com.mugames.vidsnap.Extractor;

import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.m3u8;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see Twitter is only class that can access this class
 * Pasting link from Periscope won't work but if a tweet contain Periscope it can be downloaded
 * In future it will be extended
 */
public class Periscope {


    ArrayList<ArrayList<String>> manifest;



    JSONObject data;

    Extractor extractor;


    public Periscope(Extractor extractor) {
        this.extractor = extractor;
    }

    String getID(String s) {
        Pattern pattern = Pattern.compile("https?://(?:www\\.)?(?:periscope|pscp)\\.tv/[^/]+/([^/?#]+)");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public void extractInfo(String url) {
        String id = getID(url);
        extractor.getDialogueInterface().show("Periscope video");

        manifest = new ArrayList<ArrayList<String>>();
        HttpRequest request = new HttpRequest(String.format("https://api.periscope.tv/api/v2/accessVideoPublic?broadcast_id=%s", id),
                extractor.getDialogueInterface(),response -> {
            try {
                JSONObject stream = new JSONObject(response);
                JSONObject broadcast = stream.getJSONObject("broadcast");
                data = extractData(broadcast);
                ArrayList<String> video_urls = new ArrayList<>();
                for (String format_id : new String[]{"replay", "rtmp", "hls", "https_hls", "lhls", "lhlsweb"}) {
                    String video_url = stream.getString(format_id + "_url");
                    if (nullOrEmpty(video_url) || video_urls.contains(video_url)) continue;
                    video_urls.add(video_url);
                    if (!format_id.equals("rtmp")) {
                        new m3u8(extractor).extract_m3u8(video_url, data);
                        break;
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        request.setType(HttpRequest.GET);
        request.start();
    }


    private JSONObject extractData(JSONObject broadcast) {
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
            boolean isLive = !"ENDED".equalsIgnoreCase(broadcast.getString("state"));
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