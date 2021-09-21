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

import com.mugames.vidsnap.utility.UtilityInterface.DialogueInterface;
import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.bundles.Formats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see Periscope like Periscope here also same story
 * Only for Twitter
 * Extended in future
 */
public class Twitch {
    DialogueInterface dialogueInterface;

    Hashtable<String,String> headers;

    public Twitch(DialogueInterface dialogueInterface) {
        this.dialogueInterface = dialogueInterface;
    }

    String getId(String s){
        Pattern pattern = Pattern.compile("https?://(?:clips\\.twitch\\.tv/(?:embed\\?.*?\\bclip=|(?:[^/]+/)*)|(?:(?:www|go|m)\\.)?twitch\\.tv/[^/]+/clip/)([^/?#&]+)");
        Matcher matcher=pattern.matcher(s);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    public void extractURLFromClips(String twitchURL, TwitchInfoInterface i){
        String id;
        Formats formats =new Formats();
        headers=new Hashtable<>();
        if((id= getId(twitchURL))==null){
            try {
                throw new Exception("Error!! Id can't find for URL "+twitchURL);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        headers.put("Content-Type","text/plain;charset=UTF-8");
        headers.put("Client-ID","kimne78kx3ncx6brgo4mv6wki5h1ko");
        String data=String.format("{\"query\":\"{clip(slug:\\\"%s\\\"){broadcaster{displayName}createdAt curator{displayName id}durationSeconds id tiny:thumbnailURL(width:86,height:45)small:thumbnailURL(width:260,height:147)medium:thumbnailURL(width:480,height:272)title videoQualities{frameRate quality sourceURL}viewCount}}\"}",id);
        HttpRequest request = new HttpRequest("https://gql.twitch.tv/gql",dialogueInterface,response -> {
            try {
                JSONObject clip=new JSONObject(response).getJSONObject("data").getJSONObject("clip");
                formats.thumbNailsURL.add(clip.getString("medium"));
                JSONArray videoQualities =clip.getJSONArray("videoQualities");
                formats.title= clip.getString("title");
                for (int j = 0; j< videoQualities.length(); j++){
                    JSONObject object= videoQualities.getJSONObject(j);
                    formats.mainFileURLs.add(object.getString("sourceURL"));
                    formats.fileMime.add(MIMEType.VIDEO_MP4);
                    formats.qualities.add(object.getString("quality")+"p");
                }
                i.onDone();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        request.setType(HttpRequest.POST);
        request.setHeaders(headers);
        request.start();
    }
    interface TwitchInfoInterface{
        void onDone();
    }
}