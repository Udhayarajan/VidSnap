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

package com.mugames.vidsnap;

import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.extractor.Extractor;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;


import static com.mugames.vidsnap.utility.UtilityClass.joinURL;

public class m3u8 {

    String TAG= Statics.TAG+":m3u8";


    JSONObject info;


    Extractor extractor;


    public m3u8(Extractor extractor) {
        this.extractor = extractor;
    }

    public void extract_m3u8(String url, JSONObject info){
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
                extractor.formats.fileMime.add(MIMEType.VIDEO_MP4);
                completingProcess();
            }
        });
    }



    private void real_extract(int index, String url, ChunkCallback chunkCallback) {
        HttpRequest request = new HttpRequest(url, extractor.getDialogueInterface(), response -> {
            if(response.getResponse().contains("#EXT-X-FAXS-CM:")){
                extractor.getDialogueInterface().error("Adobe Flash access can't downloaded",null);
                return;
            }
            Pattern pattern=Pattern.compile("#EXT-X-SESSION-KEY:.*?URI=\"skd://");
            if(pattern.matcher(response.getResponse()).find()){
                extractor.getDialogueInterface().error("Apple Fair Play protected can't downloaded",null);
                return;
            }
            if(response.getResponse().contains("#EXT-X-TARGETDURATION")){
                //No playlist
                extractFromMeta(index,response.getResponse(),url,chunkCallback);
            }
            else {
                extractFromPlaylist(response.getResponse(),url);
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
    }


    void extractFromPlaylist(String response, String url){
        ArrayList<String> frag_urls=new ArrayList<>();
        for(String line:response.split("\n")){
            line=line.trim();
            if(!line.startsWith("#")){
                frag_urls.add(joinURL(url,line));
                extractor.formats.chunkUrlList.add(frag_urls.get(frag_urls.size()-1));
            }
            else if(line.contains("RESOLUTION")){
                for (String l:line.split(",")){
                    l=l.trim();
                    if(l.contains("RESOLUTION")) {
                        extractor.formats.qualities.add(getResolution(l));
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
                    extractor.formats.fileMime.add(MIMEType.VIDEO_MP4);
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



    String getResolution(String query){
        return query.substring("RESOLUTION".length()+1);
    }


    interface ChunkCallback{
        void onChunkExtracted(int index, ArrayList<String> chunkURLS);
    }

}
