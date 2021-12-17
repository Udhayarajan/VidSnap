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

import android.util.Log;

import com.mugames.vidsnap.network.HttpRequest;
import com.mugames.vidsnap.utility.JSInterpreter;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.*;
import static com.mugames.vidsnap.utility.UtilityInterface.*;

public class YouTube extends Extractor {
    static final String get_video_Info = "https://www.youtube.com/get_video_info?html5=1&video_id=";
    static final String embed_Info = "https://www.youtube.com/embed/";
    static final String YT_URL = "https://www.youtube.com";

    boolean webScratch;

    String TAG = Statics.TAG + ":YouTube";

    String[] PLAYER_REGEXS = new String[]{
            "<script[^>]+\\bsrc=(\"[^\"]+\")[^>]+\\bname=[\"\\']player_ias\\/base",
            "\"jsUrl\"\\s*:\\s*(\"[^\"]+\")",
            "\"assets\":.+?\"js\":\\s*(\"[^\"]+\")"
    };
    String[] FUNCTION_REGEX = new String[]{
            "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
            "([a-zA-Z0-9$]+)\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)",
            "([\"\\'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\.sig\\|\\|([a-zA-Z0-9$]+)\\(",
            "yt\\.akamaized\\.net\\/\\)\\s*\\|\\|\\s*.*?\\s*[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\(",
            "\\bc\\s*&&\\s*a\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\("
    };

    public String videoID;


    static final String _URL_REGEX = ".+((?<=\\/).+)$";
    static final String _URL_SHORTS_REGEX = "watch\\?v=(?:(.*)&|(.*))|(.*)\\?";

    String jsFunctionName;
    String jsCode;


    int attempt = 0;

    boolean isVideoDecrypted;
    boolean isAudioDecrypted;


    public YouTube(){
        super("YouTube");
    }

    @Override
    public void analyze(String url) {
        getDialogueInterface().show("Analysing URL");
        videoID = getId(url);
        if (videoID == null) {
            getDialogueInterface().error("URL Seems to be wrong", null);
            return;
        }
        HttpRequest request = new HttpRequest(get_video_Info + videoID, getDialogueInterface(), response -> videoInfo(UtilityClass.decodeHTML(response.getResponse())));
        request.setType(HttpRequest.GET);
        request.start();

    }

    private String getId(String url) {
        String id;
        Matcher m = Pattern.compile(_URL_REGEX).matcher(url);
        if (m.find()) {
            id = m.group(1);
            if (id != null && id.contains("?")) {
                m = Pattern.compile(_URL_SHORTS_REGEX).matcher(id);
                if (m.find()) {
                    if (m.group(1) != null) return m.group(1);
                    if (m.group(2) != null) return m.group(2);
                    if (m.group(3) != null) return m.group(3);
                }
            } else return id;
        }
        return null;
    }

    int got = 0;

    void analyzeCompleted() {
        Log.d(TAG, "video updateVideoSize: ");
        fetchDataFromURLs();
    }


    public void videoInfo(String info) {
        if (info == null) {
            fetchFromWebPage();
            return;
        }

        getDialogueInterface().show("Downloading Player");
        if (jsFunctionName == null || jsFunctionName.isEmpty())
            fetchJSFile(new JSDownloaded() {
                @Override
                public void onDone() {
                    getDialogueInterface().show("Analysing data");
                    extractInfo(info);
                }
            });
        else extractInfo(info);

    }

    private void fetchFromWebPage() {
        webScratch = true;
        String YT_INITIAL_BOUNDARY_RE = "(?:var\\s+meta|</script|\\n)";
        String YT_INITIAL_PLAYER_RESPONSE_RE = "ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;";
        HttpRequest request = new HttpRequest("https://www.youtube.com/watch?v=" + videoID + "&bpctr=9999999999&has_verified=1", getDialogueInterface(), response -> {
            if(response.getResponse()==null) {
                getDialogueInterface().error("Some thing went wrong\nPlease try again", response.getException());
            }
            Matcher matcher = Pattern.compile(String.format("%s\\s*%s", YT_INITIAL_PLAYER_RESPONSE_RE, YT_INITIAL_BOUNDARY_RE)).matcher(response.getResponse());
            if (matcher.find()) videoInfo(matcher.group(1));
        });
        request.setType(HttpRequest.GET);
        request.start();
    }

    void extractInfo(String decoded) {

        try {
            JSONObject jsonObject = new JSONObject(UtilityClass.parseQueryString(decoded));
            JSONObject player_response = getObj_or_Null(jsonObject, "player_response");
            if (!webScratch && player_response == null) {
                alternateURLs(attempt++);
                return;
            } else player_response = new JSONObject(decoded);

            JSONObject streamingData = getObj_or_Null(player_response, "streamingData");
            if (streamingData == null || streamingData.toString().equals("{}")) {
                JSONObject playabilityStatus = getObj_or_Null(player_response, "playabilityStatus");
                if (playabilityStatus != null) {
                    String status = playabilityStatus.getString("status");
                    if (status.equals("LOGIN_REQUIRED")) {
                        //playabilityStatus.getString("reason")
                        getDialogueInterface().error("Age restricted videos can't be downloaded", null);
                        return;
                    } else if (status.equals("UNPLAYABLE")) {
                        getDialogueInterface().error("Movies can't be downloaded", null);
                        return;
                    }
                } else alternateURLs(attempt++);
                return;
            }
            JSONArray formatsObj = getArray_or_Null(streamingData, "adaptiveFormats");//formats


            for (int i = 0; i < formatsObj.length(); i++) {
                JSONObject selectedJSON = new JSONObject(String.valueOf(formatsObj.getJSONObject(i)));

                String unsupported = getString_or_Null(selectedJSON, "type");

                if (unsupported != null) continue;

                String mime = selectedJSON.getString("mimeType").split(";")[0];
                String urlField = getString_or_Null(selectedJSON, "url");

                final boolean isWebmAudio = mime.equals(MIMEType.AUDIO_WEBM);
                final boolean isAudio = isWebmAudio || mime.equals(MIMEType.AUDIO_MP4);


                if (!isAudio) {
                    String qual = selectedJSON.getString("qualityLabel");
                    if (formats.qualities.contains(qual))
                        continue;

                    formats.qualities.add(qual);
                }


                if (urlField == null) {
                    String signatureCipher = getString_or_Null(selectedJSON, "signatureCipher");
                    String z = UtilityClass.parseForSig(UtilityClass.decodeHTML(signatureCipher));
                    JSONObject signatureCipherObj = new JSONObject(z);
                    String sp = getString_or_Null(signatureCipherObj, "sp");
                    String s = signatureCipherObj.getString("s");
                    urlField = signatureCipherObj.getString("url");


                    if (isWebmAudio) {
                        if (formats.audioURLs.size() == 0) {
                            formats.audioSP = sp;
                            formats.audioURLs.add(urlField);
                            formats.audioSIG = s;
                            formats.audioMime.add(mime);
                        }
                    } else {
                        formats.videoSPs.add(sp);
                        formats.mainFileURLs.add(urlField);
                        formats.videoSIGs.add(s);
                        formats.fileMime.add(mime);
                    }

                } else if (isAudio) {
                    if (isWebmAudio && formats.audioURLs.isEmpty()) {
                        formats.audioURLs.add(urlField);
                        formats.audioMime.add(mime);
                    }
                } else {
                    formats.mainFileURLs.add(urlField);
                    formats.fileMime.add(mime);
                }
            }

            JSONObject videoDetails = player_response.getJSONObject("videoDetails");
            formats.title = videoDetails.getString("title").replaceAll("[\\\\/:*?\"<>|]+", "_");
            JSONObject thumbnail = videoDetails.getJSONObject("thumbnail");
            JSONArray thumbnails = thumbnail.getJSONArray("thumbnails");
            JSONObject thumbIndex = getObj_or_Null(thumbnails, 3);
            if (thumbIndex == null)
                thumbIndex = getObj_or_Null(thumbnails, thumbnails.length() - 1);
            formats.thumbNailsURL.add(thumbIndex.getString("url"));

            tryDecrypt();

        } catch (JSONException e) {
            e.printStackTrace();
            getDialogueInterface().error("Sorry this video can't be downloaded", e);
        }
    }

    void tryDecrypt() {
        if (formats.audioSIG == null) {
            analyzeCompleted();
            return;
        }

        new Thread(() -> decryptSignature(0,formats.audioSIG, formats.audioURLs.get(0), (index, decryptedSign, url) -> {
            if (formats.audioSP == null) url += ("&signature=" + decryptedSign);
            else url += ("&" + formats.audioSP + "=" + decryptedSign);
            Log.d(TAG, "Video_Info Au: \n" + url);
            formats.audioURLs.set(0, url);
            isAudioDecrypted = true;
            checkCompletion();
        })).start();

        for (int i = 0; i < formats.mainFileURLs.size(); i++) {


            String urlRaw = formats.mainFileURLs.get(i);
            String s = formats.videoSIGs.get(i);
            String sp = formats.videoSPs.get(i);


            int finalI = i;
            new Thread(() -> decryptSignature(finalI,s, urlRaw, (index, decryptedSign, url) -> {
                got++;


                if (sp == null) url += "&signature=" + decryptedSign;
                else url += ("&" + sp + "=" + decryptedSign);
                Log.d(TAG, "Video_Info: \n" + url);


                formats.mainFileURLs.set(index, url);

                boolean isLast = got == formats.mainFileURLs.size();

                if (isLast) {
                    got = 0;
                    isVideoDecrypted = true;
                    checkCompletion();
                }

            })).start();
        }


    }

    private void checkCompletion() {
        if (isVideoDecrypted && isAudioDecrypted) analyzeCompleted();
    }

    String validUrlWithSign(String url, String sp, String decryptedSign) {
        if (sp == null) url += ("&signature=" + decryptedSign);
        else url += ("&" + sp + "=" + decryptedSign);

        return url;
    }

    void alternateURLs(int attempts) {
        webScratch = false;
        String[] dataURL = new String[]{String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=embedded&ps=default&eurl=", videoID),
                String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=detailpage&ps=default&eurl=", videoID),
                String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=vevo&ps=default&eurl=", videoID)
        };
        Log.d(TAG, "alternateURLs: " + (attempts));
        getDialogueInterface().show("Attempt " + (attempts + 1));
        if (attempts >= 3) {
            getDialogueInterface().error("URL Seems to be wrong", null);
            return;
        }
        HttpRequest request = new HttpRequest(dataURL[attempts], getDialogueInterface(), response -> videoInfo(UtilityClass.decodeHTML(response.getResponse())));
        request.setType(HttpRequest.GET);
        request.start();
    }


    void fetchJSFile(JSDownloaded jsDownloaded) {
        ResponseCallBack responseCallBack = embedResponse -> {
            for (String i : PLAYER_REGEXS) {
                Pattern pattern = Pattern.compile(i);
                Matcher matcher = pattern.matcher(embedResponse.getResponse());
                if (matcher.find()) {
                    String player_url = matcher.group(1);
                    player_url = player_url==null?"":player_url.replaceAll("\"", "");

                    HttpRequest request = new HttpRequest(YT_URL+player_url,getDialogueInterface(),response -> {
                        for (String j : FUNCTION_REGEX) {
                            Pattern funPattern = Pattern.compile(j);
                            Matcher funMatcher = funPattern.matcher(response.getResponse());
                            if (funMatcher.find()) {
                                jsFunctionName = funMatcher.group(1);
                                jsCode = response.getResponse();
                                jsDownloaded.onDone();
                                break;
                            }
                        }
                    });
                    request.setType(HttpRequest.GET);
                    request.start();
                    break;
                }
            }

        };
        HttpRequest request = new HttpRequest(embed_Info+videoID,getDialogueInterface(),responseCallBack);
        request.setType(HttpRequest.GET);
        request.start();
    }

    void decryptSignature(int index, String signature, String url, SignNotifier notifier) {
        JSInterpreter jsInterpreter = new JSInterpreter(jsCode, null);
        JSInterface jsInterface = jsInterpreter.Extract_Function(jsFunctionName);
        char[] o = (char[]) jsInterface.resf(new String[]{signature});
        notifier.onDecrypted(index, UtilityClass.charArrayToSting(o), url);
    }

    public interface JSDownloaded {
        void onDone();
    }
}