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
import com.mugames.vidsnap.utility.JSInterpreterLegacy;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.utility.UtilityClass.JSONGetter.*;
import static com.mugames.vidsnap.utility.UtilityInterface.*;

public class YouTube extends Extractor {
    static final String get_video_Info = "https://www.youtube.com/get_video_info?html5=1&video_id=";
    static final String embed_Info = "https://www.youtube.com/embed/";
    static final String YT_URL = "https://www.youtube.com";
    static String YT_INITIAL_BOUNDARY_RE = "(?:var\\s+meta|</script|\\n)";
    static String YT_INITIAL_PLAYER_RESPONSE_RE = "ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;";
    static String N_FUNCTION_REGEX = "";

    boolean webScratch;

    static String TAG = Statics.TAG + ":YouTube";

    static String[] PLAYER_REGEXS = new String[]{
            "<script[^>]+\\bsrc=(\"[^\"]+\")[^>]+\\bname=[\"\\']player_ias\\/base",
            "\"jsUrl\"\\s*:\\s*(\"[^\"]+\")",
            "\"assets\":.+?\"js\":\\s*(\"[^\"]+\")"
    };
    static String[] FUNCTION_REGEX = new String[]{
            "\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\(",
            "\\bm=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)",
            "\\bc&&\\(c=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(c\\)\\)",
            "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\\\"\\\"\\s*\\);[a-zA-Z0-9$]{2}\\.[a-zA-Z0-9$]{2}\\(a,\\d+\\)",
            "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\\\"\\\"\\s*\\)",
            /*Obsolete patterns*/
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

    static String[] PLAYER_INFO_RE = new String[]{
            "s/player/([a-zA-Z0-9_-]{8,})",
            "/([a-zA-Z0-9_-]{8,})/player(?:_ias\\.vflset(?:/[a-zA-Z]{2,3}_[a-zA-Z]{2,3})?|-plasma-ias-(?:phone|tablet)-[a-z]{2}_[A-Z]{2}\\.vflset)/base\\.js$",
            "\\b(vfl[a-zA-Z0-9_-]+)\\b.*?\\.js$"
    };


    String videoID;


    static final String _URL_REGEX = ".+((?<=\\/).+)$";
    static final String _URL_SHORTS_REGEX = "watch\\?v=(?:(.*)&|(.*))|(.*)\\?";

    String jsFunctionName;
    String jsCode;

    String jsNFunctionName;


    int attempt = 0;

    boolean isVideoDecrypted;
    boolean isAudioDecrypted;


    public YouTube() {
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
        HttpRequest request = new HttpRequest(get_video_Info + videoID, response -> {
            try {
                videoInfo(UtilityClass.decodeHTML(response.getResponse()));
            } catch (JSONException e) {
                getDialogueInterface().error("Try again", e);
            }
        });
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


    void videoInfo(String info) throws JSONException {
        if (info == null) {
            fetchFromWebPage();
            return;
        }

        getDialogueInterface().show("Downloading Player");
        if (jsFunctionName == null || jsFunctionName.isEmpty())
            fetchJSFile(() -> {
                getDialogueInterface().show("Analysing data");
                try {
                    extractInfo(info);
                } catch (JSONException e) {
                    getDialogueInterface().error("Couldn't download", e);
                }
            });
        else extractInfo(info);

    }

    private void fetchFromWebPage() {
        webScratch = true;

        HttpRequest request = new HttpRequest("https://www.youtube.com/watch?v=" + videoID + "&bpctr=9999999999&has_verified=1", response -> {
            if (response.getException() != null) {
                getDialogueInterface().error(response.getResponse(), response.getException());
                return;
            }
            if (response.getResponse() == null) {
                getDialogueInterface().error("Some thing went wrong\nPlease try again", response.getException());
            }
            Matcher matcher = Pattern.compile(String.format("%s\\s*%s", YT_INITIAL_PLAYER_RESPONSE_RE, YT_INITIAL_BOUNDARY_RE)).matcher(response.getResponse());
            if (matcher.find()) {
                try {
                    videoInfo(matcher.group(1));
                } catch (JSONException e) {
                    getDialogueInterface().error("Try again", e);
                }
            }
        });
        request.setType(HttpRequest.GET);
        request.start();
    }

    void extractInfo(String decoded) throws JSONException {
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


    }

    void tryDecrypt() {
//        Still in under development
//        try {
//            unThrottleFormats();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        if (formats.audioSIG == null) {
            analyzeCompleted();
            return;
        }

        new Thread(() -> decryptSignature(0, formats.audioSIG, formats.audioURLs.get(0), (index, decryptedSign, url) -> {
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
            new Thread(() -> decryptSignature(finalI, s, urlRaw, (index, decryptedSign, url) -> {
                got++;
                if (sp == null) url += "&signature=" + decryptedSign;
                else url += ("&" + sp + "=" + decryptedSign);
                Log.d(TAG, "tryDecrypt: signature = "+decryptedSign);
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

    String extractNFunction() throws JSONException {
        String target = "([a-zA-Z0-9$]{3})(?:\\[(\\d+)\\])?";
        Matcher matcher = Pattern.compile(
                String.format("\\.get\\(\"n\"\\)\\)&&\\(b=(%s)\\([a-zA-Z0-9]\\)", target))
                .matcher(jsCode);
        if (matcher.find()) {
            String nameIdx = matcher.group(1);
            if (nameIdx != null) {
                Matcher m1 = Pattern.compile(target).matcher(nameIdx);
                if (m1.find()) {
                    String name = m1.group(1);
                    String idx = m1.group(2);
                    if (idx == null) return name;
                    else {
                        matcher = Pattern.compile(String.format("var %s\\s*=\\s*(\\[.+?\\]);", name)).matcher(jsCode);
                        if (matcher.find()) {
                            String json = matcher.group(1);
                            JSONArray jsonArray = new JSONArray(json);
                            return jsonArray.getString(Integer.parseInt(idx));
                        }
                    }
                }

            }
        }
        throw new JSONException("Unable to find function name");
    }

    void unThrottleFormats() throws JSONException {
        String funcCode = null;
        for (String signedUrl : formats.mainFileURLs) {
            Hashtable<String,String> parsed = parseUrl(signedUrl);
            if (jsNFunctionName == null) jsNFunctionName = extractNFunction();
            if (funcCode==null){
                JSInterpreter jsInterpreter = new JSInterpreter(jsCode, null);
                JSInterpreter.JSFunctionCode functionCode = jsInterpreter.extractFunctionCode(jsNFunctionName);
                JSInterface jsInterface = jsInterpreter.extractFunctionFromCode(functionCode);
                Object o = jsInterface.resf(new Object[]{parsed.get("n")});
                Log.d(TAG, "unThrottleFormats: "+String.valueOf(o));
            }
        }
    }

    private Hashtable<String, String> parseUrl(String signedUrl) {
        String[] parsed = signedUrl.split("&");
        Hashtable<String, String> hashtable = new Hashtable<>();
        for (String i : parsed) {
            String[] temp = i.split("=");
            hashtable.put(temp[0], temp[1]);
        }
        return hashtable;
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
        HttpRequest request = new HttpRequest(dataURL[attempts], response -> {
            if (response.getException() != null) {
                getDialogueInterface().error(response.getResponse(), response.getException());
                return;
            }
            try {
                videoInfo(UtilityClass.decodeHTML(response.getResponse()));
            } catch (JSONException e) {
                getDialogueInterface().error("Internal error", e);
            }
        });
        request.setType(HttpRequest.GET);
        request.start();
    }


    void fetchJSFile(JSDownloaded jsDownloaded) {
        ResponseCallBack responseCallBack = embedResponse -> {
            if (embedResponse.getException() != null) {
                getDialogueInterface().error(embedResponse.getResponse(), embedResponse.getException());
                return;
            }
            for (String i : PLAYER_REGEXS) {
                Pattern pattern = Pattern.compile(i);
                Matcher matcher = pattern.matcher(embedResponse.getResponse());
                if (matcher.find()) {
                    String player_url = matcher.group(1);
                    player_url = player_url == null ? "" : player_url.replaceAll("\"", "");

                    HttpRequest request = new HttpRequest(YT_URL + player_url, response -> {
                        if (response.getException() != null) {
                            getDialogueInterface().error(response.getResponse(), response.getException());
                            return;
                        }
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
        HttpRequest request = new HttpRequest(embed_Info + videoID, responseCallBack);
        request.setType(HttpRequest.GET);
        request.start();
    }

    void decryptSignature(int index, String signature, String url, SignNotifier notifier) {
        JSInterpreterLegacy jsInterpreter = new JSInterpreterLegacy(jsCode, null);
        JSInterface jsInterface = jsInterpreter.extractFunction(jsFunctionName);
        char[] o = (char[]) jsInterface.resf(new String[]{signature});
        notifier.onDecrypted(index, UtilityClass.charArrayToSting(o), url);
    }

    public interface JSDownloaded {
        void onDone();
    }
}