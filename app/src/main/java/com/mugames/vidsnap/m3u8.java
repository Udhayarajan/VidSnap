package com.mugames.vidsnap;

import android.graphics.Bitmap;
import android.util.Log;

import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.Utility.UtilityInterface.AnalyzeCallback;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;


import static com.mugames.vidsnap.Utility.UtilityClass.joinURL;

public class m3u8 {

    String TAG= Statics.TAG+":m3u8";

    MainActivity activity;

    Formats formats;

    //variables needed reset
    ArrayList<String> chunksURL;
    ArrayList<ArrayList<String>> manifest;
    ArrayList<String> playList_resolutions;

    JSONObject info;
    Bitmap thumbNail;
    String fileSize;
    AnalyzeCallback analyzeCallback;

    String cacheFileName;

    public m3u8(MainActivity activity) {
        this.activity = activity;
    }

    public void Extract_m3u8(String url, JSONObject info, AnalyzeCallback analyzeCallback){
        this.analyzeCallback=analyzeCallback;
        try {
            if(info.get("isLive").equals("true")){
                activity.error("Live video can't be downloaded",null);
                return;
            }
        }catch (JSONException e){}
        activity.dialog.show("This may take a while.");
        Reset();
        this.info=info;



        real_extract(url, new chunkCallback() {
            @Override
            public void onChunkExtracted(ArrayList<String> chunkURLS) {
                manifest.add(chunkURLS);
                GetSizes();
            }
        });
    }

    void Reset(){
        manifest=new ArrayList<>();
        playList_resolutions=new ArrayList<>();
        formats =new Formats();
    }

    private void real_extract(String url, chunkCallback chunkCallback) {
        new HttpRequest(activity, url, null, null,
                null, null,null, new UtilityInterface.ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                if(response.contains("#EXT-X-FAXS-CM:")){
                    activity.error("Adobe Flash access can't downloaded",null);
                    return;
                }
                Pattern pattern=Pattern.compile("#EXT-X-SESSION-KEY:.*?URI=\"skd://");
                if(pattern.matcher(response).find()){
                    activity.error("Apple Fair Play protected can't downloaded",null);
                    return;
                }
                if(response.contains("#EXT-X-TARGETDURATION")){
                    //No playlist
                    ExtractFromMeta(response,url,chunkCallback);
                }
                else {
                    ExtractFromPlaylist(response,url);
                }
            }
        }).start();

    }

    //size
    int index=0;
    int sizes=0;
    int got=0;
    void GetSizes(){
        ArrayList<String> chuncks=manifest.get(index);
        for(int i=0;i<chuncks.size();i++){
            new MiniExecute(activity, chuncks.get(i), true,i, new UtilityInterface.MiniExecutorCallBack() {
                @Override
                public void onBitmapReceive(Bitmap image) {

                }

                @Override
                public void onSizeReceived(int size,int position){
                    sizes+=size;
                    got++;

                    if(got<chuncks.size()){
                        float percen=(float) got /(float) chuncks.size();
                        percen+=((float)index/(float) manifest.size());
                        DecimalFormat decimalFormat=new DecimalFormat("0");
                        activity.dialog.show(String.format("Calculating Size(%s%%)",decimalFormat.format((percen/2f)*100)));
                    }
                    else {
                        Log.d(TAG, "FULLY FOUND onSizeReceived: "+sizes);
                        Log.e(TAG, "onSizeReceived: ");
                        formats.raw_quality_size.add(String.valueOf(sizes));
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        formats.quality_size.add(decimalFormat.format(sizes / Math.pow(10, 6)));
                        sizes=0;
                        got=0;
                        if(index<manifest.size()-1){
                            index++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            GetSizes();
                            return;
                        }
                        index=0;
                        try {
                            activity.dialog.show("Almost Done!!");
                            new MiniExecute(activity, info.getString("thumbNailURL"), false,0, new UtilityInterface.MiniExecutorCallBack() {
                                @Override
                                public void onBitmapReceive(Bitmap image) {
                                    formats.thumbNailBit=image;
                                    SetUpM3U8();
                                }

                                @Override
                                public void onSizeReceived(int size,int isLast) {

                                }
                            }).start();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();

        }
    }

    void SetUpM3U8(){
        try {
            formats.title=info.getString("title");
        }catch (JSONException e){
            formats.title="Stream_video";
        }

        for(int i=0;i<formats.quality_size.size();i++){
            if(manifest.size()==1) {
                try {
                    formats.qualities.add(info.getString("resolution"));
                } catch (JSONException e) {
                    formats.qualities.add("--");
                }
            }
            else{
                if(playList_resolutions.size()==manifest.size()){
                    formats.qualities.add(playList_resolutions.get(i));
                }
            }
        }
        formats.src="Twitter";
        analyzeCallback.onAnalyzeCompleted(formats,true);

    }

    void ExtractFromPlaylist(String response,String url){
        ArrayList<String> frag_urls=new ArrayList<>();
        for(String line:response.split("\n")){
            line=line.trim();
            if(!line.startsWith("#")){
                frag_urls.add(joinURL(url,line));
            }
            else if(line.contains("RESOLUTION")){
                for (String l:line.split(",")){
                    l=l.trim();
                    if(l.contains("RESOLUTION")) {
                        Log.d(TAG, "FULLY FOUND ExtractFromPlaylist: " + GetValue("RESOLUTION", l));
                        playList_resolutions.add(GetValue("RESOLUTION", l));
                    }
                }
            }
        }

        manifest=new ArrayList<>();

        for (int i=0;i<frag_urls.size();i++) {
            Log.d(TAG, "ExtractFromPlaylist: "+frag_urls.get(i));
            manifest.add(null);
        }
        for (String s:frag_urls){
            real_extract(s, new chunkCallback() {
                @Override
                public void onChunkExtracted(ArrayList<String> chunkURLS) {
                    int frag_index=frag_urls.indexOf(s);
                    manifest.set(frag_index,chunkURLS);
                    got++;
                    Log.d(TAG, "onChunkExtracted: "+got);
                    if(got==frag_urls.size()){
                        got=0;
                        GetSizes();
                    }
                }
            });

        }
    }


    private void ExtractFromMeta(String meta,String url,chunkCallback chunkCallback) {
        ArrayList<String> chunksUrl = new ArrayList<>();
        int mediaFrag=0;
        int ad_frag=0;
        boolean ad_frag_next=false;
        if (canDownload(meta)) {
            for (String line : meta.split("\n")) {
                line=line.trim();
                if (nullOrEmpty(line)) continue;
                if(line.startsWith("#")){
                    if (is_ad_fragment_start(line))
                        ad_frag_next = true;
                    else if (is_ad_fragment_end(line))
                        ad_frag_next = false;
                    continue;
                }
                if(ad_frag_next) {
                    ad_frag++;
                    continue;
                }
                mediaFrag+=1;

            }
            int i=0;
            int media_sequence=0;
            int frag_index = 0;
            ad_frag_next = false;
            for (String line : meta.split("\n")){
                line=line.trim();
                if(!nullOrEmpty(line)){
                    if(!line.startsWith("#")){
                        if(ad_frag_next) continue;;
                        chunksUrl.add(find_url(url,line));
                    }
                    else if (line.startsWith("#EXT-X-KEY")){

                    }
                    else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")){
                        media_sequence= Integer.parseInt(line.substring(22));
                    }
                    else if(line.startsWith("#EXT-X-BYTERANGE")){

                    }
                    else if (is_ad_fragment_start(line)) ad_frag_next=true;
                    else if(is_ad_fragment_end(line)) ad_frag_next=false;
                }
            }
        }
        chunkCallback.onChunkExtracted(chunksUrl);
    }

    private boolean canDownload(String meta) {
        for (String s:new String[]{"#EXT-X-KEY:METHOD=(?!NONE|AES-128)","#EXT-X-MAP:"}){
            Pattern pattern=Pattern.compile(s);
            if(pattern.matcher(meta).find()){
                return false;
            }
        }
        if (meta.contains("#EXT-X-KEY:METHOD=AES-128")) return false;
        return !meta.contains("#EXT-X-BYTERANGE");

    }

    boolean is_ad_fragment_start(String metaLine){
        return metaLine.startsWith("#ANVATO-SEGMENT-INFO") && metaLine.contains("type=ad") || metaLine.startsWith("#UPLYNK-SEGMENT") && metaLine.endsWith(",ad");
    }

    boolean is_ad_fragment_end(String metaLine){
        return metaLine.startsWith("#ANVATO-SEGMENT-INFO") && metaLine.contains("type=master") || metaLine.startsWith("#UPLYNK-SEGMENT") && metaLine.endsWith(",segment");
    }

    boolean nullOrEmpty(String s){
        return s == null || s.isEmpty();
    }


    String find_url(String mainURL,String line){
        if(Pattern.compile("^https?://").matcher(line).find()) return line;
        return joinURL(mainURL,line);
    }



    String GetValue(String name,String query){
        return query.substring(name.length()+1);
    }


    interface chunkCallback{
        void onChunkExtracted(ArrayList<String> chunkURLS);
    }

}
