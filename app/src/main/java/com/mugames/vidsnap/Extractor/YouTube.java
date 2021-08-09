package com.mugames.vidsnap.Extractor;

import android.graphics.Bitmap;
import android.util.Log;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.Threads.HttpRequest;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.Extractor;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.JSInterpreter;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mugames.vidsnap.Utility.UtilityClass.JSONGetter.*;
import static com.mugames.vidsnap.Utility.UtilityInterface.*;

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

    public String ID = "";


    static final String _URL_REGEX = ".+((?<=\\/).+)$";
    static final String _URL_SHORTS_REGEX = "watch\\?v=(?:(.*)&|(.*))|(.*)\\?";

    String jsFunctionName;
    String jsCode;
    MainActivity activity;

    int attempt = 0;


    AnalyzeCallback analyzeCallback;
    Bitmap thumbNail = null;
    final boolean[] gotMedia = new boolean[]{false, false};//0-audio ;1-video


    Formats formats;

    public YouTube(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void Analyze(String url, AnalyzeCallback analyzeCallback) {
        activity.dialog.show("Analysing URL");
        this.analyzeCallback = analyzeCallback;
        String videoID = GetId(url);
        if (videoID == null) {
            activity.error("URL Seems to be wrong",null);
            return;
        }
        if (!videoID.equals(ID)) {
            Reset();
            new HttpRequest(activity, get_video_Info + videoID, null, null,
                    null, null, null, new ResponseCallBack() {
                @Override
                public void onReceive(String response) {
                    ID = videoID;
                    Video_Info(UtilityClass.decodeHTML(response));
                }
            }).start();
        }
    }

    void Reset() {
        thumbNail = null;
        formats = new Formats();
    }

    private String GetId(String url) {
        String id;
        Matcher m = Pattern.compile(_URL_REGEX).matcher(url);
        if (m.find()) {
            id = m.group(1);
            if (id.contains("?")) {
                m = Pattern.compile(_URL_SHORTS_REGEX).matcher(id);
                if (m.find()) {
                    if (m.group(1) != null) return m.group(1);
                    if (m.group(2) != null) return m.group(2);
                    if (m.group(3) != null) return m.group(3);
                }
            } else return id;
        }
        return null;
//        if (url.contains(".be")||url.contains("/")) id= url.replaceAll(".+?(?:/)","");
//        else if (url.contains(".com")) id=url.replaceAll(".+?(?:=)", "");
//        else {
//            Log.d(TAG, "GetId: "+url.replaceAll(".+?(?:=)", ""));
//        }
//        Log.d(TAG, "GetId: "+id);
//        return id;
    }

    int got = 0;

    void UpdateUI() {
        Log.d(TAG, "video updateVideoSize: ");
        if (got < formats.videoURLs.size()) {
            new MiniExecute(activity, formats.videoURLs.get(got), true, 0, new MiniExecutorCallBack() {

                @Override
                public void onBitmapReceive(Bitmap image) {

                }

                @Override
                public void onSizeReceived(int size, int isLast) {
                    got++;
                    Log.d(TAG, "video onSizeReceived: got size" + size);
                    formats.raw_quality_size.add(String.valueOf(size));
                    DecimalFormat decimalFormat = new DecimalFormat("#.##");
                    formats.quality_size.add(decimalFormat.format(size / Math.pow(10, 6)));
                    UpdateUI();
                }
            }).start();
            return;
        }

//        QualityFragment dialogFragment= QualityFragment.newInstance(formats.qualities.size(),formats.qualities,formats.quality_size);
//        dialogFragment.setRequired(activity, new DownloadButtonCallBack() {
//            @Override
//            public void onDownloadButtonPressed(String fileName,String path) {
//                Download(fileName,path);
//            }
//
//            @Override
//            public void onSelectedItem(int position, QualityFragment qualityFragment) {
//                Log.d(TAG, "onSelectedItem: "+position);
//
//                videoURL =formats.videoURLs.get(position);
//                video_mime=formats.mimeTypes_video.get(position);
//
//                fileSize=formats.raw_quality_size.get(position);
//
//                cacheFileName = UtilityClass.GetValidFile(directory+videoName+"_"+formats.qualities.get(position)+"_"+".mp4");
//                qualityFragment.setName(cacheFileName.replaceAll(directory,""),directory);
//            }
//        });
        attempt = 0;
        Download();
//        dialogFragment.setThumbNail(thumbNail);
//        dialogFragment.show(activity.getSupportFragmentManager(),TAG);
    }


    public void Download() {
        formats.thumbNailBit = thumbNail;
        formats.src = "YouTube";
        analyzeCallback.onAnalyzeCompleted(formats, true);
    }


    public void Video_Info(String info) {
        if (info == null) {
            fetchFromWebPage();
            return;
        }

        activity.dialog.show("Downloading Player");
        if (jsFunctionName == null || jsFunctionName.isEmpty())
            GetJSFile(new JSDownloaded() {
                @Override
                public void onDone() {
                    activity.dialog.show("Analysing data");
                    ExtractInfo(info);
                }
            });
        else ExtractInfo(info);

    }

    private void fetchFromWebPage() {
        webScratch=true;
        String YT_INITIAL_BOUNDARY_RE = "(?:var\\s+meta|</script|\\n)";
        String YT_INITIAL_PLAYER_RESPONSE_RE = "ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;";
        new HttpRequest(activity, "https://www.youtube.com/watch?v=" + ID + "&bpctr=9999999999&has_verified=1",
                null, null, null, null, null, new ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                Matcher matcher = Pattern.compile(String.format("%s\\s*%s", YT_INITIAL_PLAYER_RESPONSE_RE, YT_INITIAL_BOUNDARY_RE)).matcher(response);
                if (matcher.find()) {
                    Video_Info(matcher.group(1));
                }
            }
        }).start();
    }

    void ExtractInfo(String decoded) {

        try {
            if (formats.qualities.size() > 0) formats.qualities.clear();

            File file = new File(activity.getExternalFilesDir(null) + "/json" + attempt + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(UtilityClass.parseQueryString(decoded));
            writer.close();
            JSONObject jsonObject = new JSONObject(UtilityClass.parseQueryString(decoded));
            JSONObject player_response = getObj_or_Null(jsonObject, "player_response");
            if (!webScratch && player_response == null) {
                OtherURLs(attempt++);
                return;
            }else player_response= new JSONObject(decoded);

            JSONObject streamingData = getObj_or_Null(player_response, "streamingData");
            if (streamingData == null || streamingData.toString().equals("{}")) {
                JSONObject playabilityStatus =  getObj_or_Null(player_response,"playabilityStatus");
                if(playabilityStatus!=null){
                    String status = playabilityStatus.getString("status");
                    if(status.equals("LOGIN_REQUIRED")) {
                        //playabilityStatus.getString("reason")
                        activity.error("Age restricted videos can't be downloaded",null);
                        return;
                    }else if(status.equals("UNPLAYABLE")){
                        activity.error("Movies can't be downloaded",null);
                        return;
                    }
                }
                else OtherURLs(attempt++);
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
                        if (formats.audioURL == null) {
                            formats.audioSP = sp;
                            formats.audioURL = urlField;
                            formats.audioSIG = s;
                            formats.mimeType_audio = mime;
                        }
                    } else {
                        formats.videoSPs.add(sp);
                        formats.videoURLs.add(urlField);
                        formats.videoSIGs.add(s);
                        formats.mimeTypes_video.add(mime);
                    }

                } else if (isAudio) {
                    if (isWebmAudio && (formats.audioURL == null || formats.audioURL.isEmpty())) {
                        formats.audioURL = urlField;
                        formats.mimeType_audio = mime;
                    }
                } else {
                    formats.videoURLs.add(urlField);
                    formats.mimeTypes_video.add(mime);
                }
            }

            if (thumbNail == null) {
                JSONObject videoDetails = player_response.getJSONObject("videoDetails");
                formats.title = videoDetails.getString("title").replaceAll("[\\\\/:*?\"<>|]+", "_");
                JSONObject thumbnail = videoDetails.getJSONObject("thumbnail");
                JSONArray thumbnails = thumbnail.getJSONArray("thumbnails");
                JSONObject thumbIndex = getObj_or_Null(thumbnails,3);
                if(thumbIndex == null) thumbIndex = getObj_or_Null(thumbnails,thumbnails.length()-1);
                new MiniExecute(activity, thumbIndex.getString("url"), false, 0, new MiniExecutorCallBack() {
                    @Override
                    public void onBitmapReceive(Bitmap image) {
                        thumbNail = image;
                        if (gotMedia[0] && gotMedia[1]) {
                            Log.d(TAG, "onDecrypted: Update calling got thumbnail");
                            UpdateUI();
                        }
                    }

                    @Override
                    public void onSizeReceived(int size, int isLast) {

                    }
                }).start();
            }

            activity.dialog.show("Almost Done!!");

            tryDecrpytAndDownload();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            activity.error("Sorry this video can't be downloaded",e);
        }
    }

    void tryDecrpytAndDownload() {
        if (formats.audioSIG == null) {
            if (thumbNail != null) {
                Log.d(TAG, "onDecrypted: Update calling null sig");
                UpdateUI();
            } else {
                gotMedia[0] = true;
                gotMedia[1] = true;
            }
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                DecryptSignature(formats.audioSIG, formats.audioURL, new SignNotifier() {
                    @Override
                    public void onDecrypted(String decryptedSign, String url) {
                        if (formats.audioSP == null) url += ("&signature=" + decryptedSign);
                        else url += ("&" + formats.audioSP + "=" + decryptedSign);
                        Log.d(TAG, "Video_Info Au: \n" + url);
                        formats.audioURL = url;
                        if (gotMedia[1] && thumbNail != null) {
                            Log.d(TAG, "onDecrypted: Update calling audio urls");
                            UpdateUI();
                        } else gotMedia[0] = true;
                    }
                });
            }
        }).start();

        for (int i = 0; i < formats.videoURLs.size(); i++) {


            String urlRaw = formats.videoURLs.get(i);
            String s = formats.videoSIGs.get(i);
            String sp = formats.videoSPs.get(i);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    DecryptSignature(s, urlRaw, new SignNotifier() {
                        @Override
                        public void onDecrypted(String decryptedSign, String url) {
                            got++;

                            int finalI = formats.videoURLs.indexOf(url);

                            if (sp == null) url += ("&signature=" + decryptedSign);
                            else url += ("&" + sp + "=" + decryptedSign);
                            Log.d(TAG, "Video_Info: \n" + url);


                            formats.videoURLs.set(finalI, url);

                            boolean isLast = got == formats.videoURLs.size();

                            if (isLast) {
                                got = 0;
                                if (gotMedia[0] && thumbNail != null) {
                                    Log.d(TAG, "onDecrypted: Update calling video urls");
                                    UpdateUI();
                                } else
                                    gotMedia[1] = true;
                            }

                        }
                    });

                }
            }).start();
        }


    }

    String validUrlWithSign(String url, String sp, String decryptedSign) {
        if (sp == null) url += ("&signature=" + decryptedSign);
        else url += ("&" + sp + "=" + decryptedSign);

        return url;
    }

    void OtherURLs(int attempts) {
        webScratch=false;
        String[] dataURL = new String[]{String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=embedded&ps=default&eurl=", ID),
                String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=detailpage&ps=default&eurl=", ID),
                String.format("https://www.youtube.com/get_video_info?html5=1&video_id=%s&el=vevo&ps=default&eurl=", ID)
        };
        Log.d(TAG, "OtherURLs: " + (attempts));
        activity.dialog.show("Attempt " + (attempts + 1));
        if (attempts >= 3) {
            activity.error("URL Seems to be wrong",null);
            return;
        }
        new HttpRequest(activity, dataURL[attempts], null, null,
                null, null, null, new ResponseCallBack() {
            @Override
            public void onReceive(String response) {
                Video_Info(UtilityClass.decodeHTML(response));
            }
        }).start();
    }


    void GetJSFile(JSDownloaded jsDownloaded) {
        new HttpRequest(activity, embed_Info + ID, null,
                null, null, null, null, new ResponseCallBack() {
            @Override
            public void onReceive(String embedResponse) {

                for (String i : PLAYER_REGEXS) {
                    Pattern pattern = Pattern.compile(i);
                    Matcher matcher = pattern.matcher(embedResponse);
                    if (matcher.find()) {
                        String player_url = matcher.group(1);
                        Log.d(TAG, "onReceive: \n" + YT_URL + (player_url.replaceAll("\"", "")));
                        new HttpRequest(activity, YT_URL + (player_url.replaceAll("\"", "")), null,
                                null, null, null, null, new ResponseCallBack() {
                            @Override
                            public void onReceive(String response) {
                                for (String j : FUNCTION_REGEX) {
                                    Pattern funPattern = Pattern.compile(j);
                                    Matcher funMatcher = funPattern.matcher(response);
                                    if (funMatcher.find()) {
                                        jsFunctionName = funMatcher.group(1);
                                        jsCode = response;
                                        jsDownloaded.onDone();
                                        break;
                                    }
                                }

                            }
                        }).start();
                        break;
                    }
                }
            }
        }).start();
    }

    void DecryptSignature(String signature, String url, SignNotifier notifier) {
        Log.d(TAG, "DecryptSignature: " + signature);
        JSInterpreter jsInterpreter = new JSInterpreter(jsCode, null);
        JSInterface jsInterface = jsInterpreter.Extract_Function(jsFunctionName);
        char[] o = (char[]) jsInterface.resf(new String[]{signature});
        notifier.onDecrypted(UtilityClass.charArrayToSting(o), url);
    }

    public interface JSDownloaded {
        void onDone();
    }
}