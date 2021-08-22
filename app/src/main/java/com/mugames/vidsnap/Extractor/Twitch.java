package com.mugames.vidsnap.Extractor;

import android.content.DialogInterface;

import com.mugames.vidsnap.Utility.UtilityInterface.DialogueInterface;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.Utility.Formats;

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

    String GetId(String s){
        Pattern pattern = Pattern.compile("https?://(?:clips\\.twitch\\.tv/(?:embed\\?.*?\\bclip=|(?:[^/]+/)*)|(?:(?:www|go|m)\\.)?twitch\\.tv/[^/]+/clip/)([^/?#&]+)");
        Matcher matcher=pattern.matcher(s);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    public void ExtractURLFromClips(String twitchURL,TwitchInfoInterface i){
        String id;
        Formats formats =new Formats();
        headers=new Hashtable<>();
        if((id=GetId(twitchURL))==null){
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
                    formats.videoURLs.add(object.getString("sourceURL"));
                    formats.videoMime.add(MIMEType.VIDEO_MP4);
                    formats.qualities.add(object.getString("quality")+"p");
                }
                i.onDone(formats);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        request.setType(HttpRequest.POST);
        request.setHeaders(headers);
        request.start();
    }
    interface TwitchInfoInterface{
        void onDone(Formats formats);
    }
}