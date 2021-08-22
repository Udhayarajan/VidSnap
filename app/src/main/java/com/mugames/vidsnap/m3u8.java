package com.mugames.vidsnap;

import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Utility.Extractor;
import com.mugames.vidsnap.Utility.Statics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;


import static com.mugames.vidsnap.Utility.UtilityClass.joinURL;

public class m3u8 {

    String TAG= Statics.TAG+":m3u8";


    JSONObject info;


    Extractor extractor;


    public m3u8(Extractor extractor) {
        this.extractor = extractor;
    }

    public void Extract_m3u8(String url, JSONObject info){
        try {
            if(info.get("isLive").equals("true")){
                extractor.getDialogueInterface().error("Live video can't be downloaded",null);
                return;
            }
        }catch (JSONException e){}
        extractor.getDialogueInterface().show("This may take a while.");
        this.info=info;


        real_extract(0,url, new ChunkCallback() {
            @Override
            public void onChunkExtracted(int index, ArrayList<String> chunkURLS) {
                //Called only if it is not playlist else completingProcess() directly called from extractFromPlaylist
                extractor.formats.manifest.add(chunkURLS);
                completingProcess();
            }
        });
    }



    private void real_extract(int index, String url, ChunkCallback chunkCallback) {
        HttpRequest request = new HttpRequest(url, extractor.getDialogueInterface(), response -> {
            if(response.contains("#EXT-X-FAXS-CM:")){
                extractor.getDialogueInterface().error("Adobe Flash access can't downloaded",null);
                return;
            }
            Pattern pattern=Pattern.compile("#EXT-X-SESSION-KEY:.*?URI=\"skd://");
            if(pattern.matcher(response).find()){
                extractor.getDialogueInterface().error("Apple Fair Play protected can't downloaded",null);
                return;
            }
            if(response.contains("#EXT-X-TARGETDURATION")){
                //No playlist
                extractFromMeta(index,response,url,chunkCallback);
            }
            else {
                extractFromPlaylist(response,url);
            }
        });
        request.setType(HttpRequest.GET);
        request.start();

    }

    //size
    int got=0;
    void completingProcess(){
        try {
            extractor.formats.title=info.getString("title");
        }catch (JSONException e){
            extractor.formats.title="Stream_video";
        }

        if(extractor.formats.manifest.size()==1) {
            try {
                extractor.formats.qualities.add(info.getString("resolution"));
            } catch (JSONException e) {
                extractor.formats.qualities.add("--");
            }
        }
        try {
            extractor.formats.thumbNailsURL.add(info.getString("thumbNailURL"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        extractor.fetchDataFromURLs();

//        for(int i=0;i<chuncks.size();i++){
//            UtilityInterface.ResponseCallBack responseCallBack = response -> {
//
//            };
//
//            HttpRequest request = new HttpRequest()
//            new MiniExecute(activity, chuncks.get(i), true,i, new UtilityInterface.MiniExecutorCallBack() {
//                @Override
//                public void onBitmapReceive(Bitmap image) {
//
//                }
//
//                @Override
//                public void onSizeReceived(long size, int position){
//                    sizes+=size;
//                    got++;
//
//                    if(got<chuncks.size()){
//                        float percen=(float) got /(float) chuncks.size();
//                        percen+=((float)index/(float) manifest.size());
//                        DecimalFormat decimalFormat=new DecimalFormat("0");
//                        activity.dialog.show(String.format("Calculating Size(%s%%)",decimalFormat.format((percen/2f)*100)));
//                    }
//                    else {
//                        Log.d(TAG, "FULLY FOUND onSizeReceived: "+sizes);
//                        Log.e(TAG, "onSizeReceived: ");
//                        formats.fileSizes.add(sizes);
//                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
//                        formats.fileSizeInString.add(decimalFormat.format(sizes / Math.pow(10, 6)));
//                        sizes=0;
//                        got=0;
//                        if(index<manifest.size()-1){
//                            index++;
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            completingProcess();
//                            return;
//                        }
//                        index=0;
//                        try {
//                            activity.dialog.show("Almost Done!!");
//                            new MiniExecute(activity, , false,0, new UtilityInterface.MiniExecutorCallBack() {
//                                @Override
//                                public void onBitmapReceive(Bitmap image) {
//                                    formats.thumbNailBit=image;
//                                    setUpM3U8();
//                                }
//
//                                @Override
//                                public void onSizeReceived(long size, int isLast) {
//
//                                }
//                            }).start();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                }
//            }).start();
//
//        }
    }


    void extractFromPlaylist(String response, String url){
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
                        extractor.formats.qualities.add(GetValue("RESOLUTION", l));
                    }
                }
            }
        }


        for (int i=0;i<frag_urls.size();i++) {
            extractor.formats.manifest.add(null);
            real_extract(i,frag_urls.get(i), new ChunkCallback() {
                @Override
                public void onChunkExtracted(int index, ArrayList<String> chunkURLS) {
                    extractor.formats.manifest.set(index,chunkURLS);
                    got++;
                    if(got==frag_urls.size()){
                        got=0;
                        completingProcess();
                    }
                }
            });

        }
    }


    private void extractFromMeta(int index, String meta, String url, ChunkCallback chunkCallback) {
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
                        if(ad_frag_next) continue;
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
//        extractor.formats.manifest.add(chunksUrl);
        chunkCallback.onChunkExtracted(index,chunksUrl);
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


    interface ChunkCallback{
        void onChunkExtracted(int index, ArrayList<String> chunkURLS);
    }

}
