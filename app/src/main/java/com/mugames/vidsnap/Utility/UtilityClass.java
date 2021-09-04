package com.mugames.vidsnap.Utility;


import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilityClass {


    public static String parseQueryString(String query) {
        int preExist = 0;
        boolean value = false;
        boolean secJson = false;
        if(query.startsWith("{") && query.endsWith("}")) return query;
        StringBuilder parsed = new StringBuilder();
        for (char ch : query.toCharArray()) {
            if (ch == '{') preExist += 1;
            else if (ch == '}') preExist -= 1;
            else if (preExist == 0) {
                if (ch == '=') {
                    if (!value)
                        parsed.append("\"").append(":").append("\"");
                    value = true;
                } else if (ch == '&') {
                    if (value) value = false;
                    if (secJson) {
                        parsed.append("},\"");
                        secJson = false;
                    } else {
                        parsed.append("\",\"");
                    }
                } else parsed.append(ch);
            }
            if (preExist > 0) {
                parsed.append(ch);
                if (!secJson) {
                    if ((parsed.charAt(parsed.length() - 2)) != ':') {
                        parsed.deleteCharAt(parsed.length() - 2);
                    }
                    secJson = true;
                }
            }
        }
        if (parsed.charAt(parsed.length() - 1) == '}' || parsed.charAt(parsed.length() - 1) == ']') {
            return "{\"" + parsed + "}}";
        }
        return "{\"" + parsed + "\"}";
    }

    public static String parseForSig(String query) {
        Pattern pattern = Pattern.compile("((?<=s\\=).*?\\s*(?=[\\&])).*((?<=sp\\=).*?\\s*(?=[\\&])).*((?<=url\\=).*)");
        Matcher matcher = pattern.matcher(query);
        StringBuilder parsed = new StringBuilder();

        if (matcher.find()) {
            parsed.append("\"s\":\"").append(matcher.group(1)).append("\",");
            if (matcher.group(3) != null)
                parsed.append("\"sp\":\"").append(matcher.group(2)).append("\",");
            else {
                parsed.append("\"url\":\"").append(matcher.group(2)).append("\"");
                return "{" + parsed.toString() + "}";
            }
            parsed.append("\"url\":\"").append(matcher.group(3)).append("\"");
        }

        return "{" + parsed.toString() + "}";
    }

    public static String removeQuotes(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.length() < 2) return s;
        for (char c : new char[]{'\'', '"'}) {
            if (s.charAt(0) == c && s.charAt(s.length() - 1) == c) {
                return s.substring(1, s.length() - 2);
            }
        }
        return s;
    }

    public static char[] joinArray(char[] a, char[] b) {
        if (a[0] == '"' && a[1] == '"') return b;
        char[] s = new char[a.length + b.length];
        System.arraycopy(a, 0, s, 0, a.length);
        System.arraycopy(b, 0, s, a.length, b.length);
        return s;
    }

    public static String charArrayToSting(char[] chars) {
        StringBuilder s = new StringBuilder();
        for (char c : chars) {
            if (c == '[' || c == ']' || c == ' ' || c == ',') {
                continue;
            }
            s.append(c);
        }
        return s.toString();
    }

    public static char[] popCharArray(char[] chars, int index) {
        if (chars == null || index < 0 || index > chars.length) return chars;
        char[] x = new char[chars.length - 1];
        System.arraycopy(chars, 0, x, 0, index);
        System.arraycopy(chars, index + 1, x, index, (chars.length - index) - 1);
        return x;
    }

    public static char[] stringToCharArray(String s) {
        char[] chars = new char[s.length()];
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == ']' || c == ' ' || c == ',') {
                continue;
            }
            chars[i] = c;
        }
        return chars;
    }

    public static char[] reverse(char[] a) {
        char[] b = new char[a.length];
        int j = a.length;
        for (int i = 0; i < a.length; i++, j--) {
            b[j - 1] = a[i];
        }
        return b;
    }


    private static String Escape_regex(String fileName) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fileName.length(); i++) {
            for (char c : new char[]{'.', '(', ')'}) {
                char ch = fileName.charAt(i);
                if (ch == c) builder.append("\\");
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String decodeHTML(String text) {
        if(text==null)
            return null;

        String decoded;


        decoded = HtmlCompat.fromHtml(text,HtmlCompat.FROM_HTML_MODE_LEGACY).toString();


        try {
            decoded = URLDecoder.decode(decoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decoded;
    }

    public static String joinURL(String url, String uRL) {
        try {
            URI uri = new URI(url);
            URL joined = uri.resolve(uRL.trim()).toURL();
            return joined.toString();
        } catch (Exception e) {
            System.out.println("[join] " + e);
            return null;
        }
    }


    public static String Escape(String s) {
        for (String c : new String[]{"#", "$", "&", "(", ")", "*", "+", "-", ".", "?", "[", "\\", "]", "^", "{", "|", "}", "~"}) {
            if (s.equals(c)) return "\\" + s;
        }
        return s;
    }

    public static byte[] bitmapToBytes(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }



    public static Bitmap bytesToBitmap(byte[] bitmapData, int imageWidth, int imageHeight) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
        if (bitmap == null) {
            Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            ByteBuffer buffer = ByteBuffer.wrap(bitmapData);
            bmp.copyPixelsFromBuffer(buffer);
        }
        return bitmap;
    }

    public static String formatFileSize(long size, boolean isSpeed) {
        String hrSize;

        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0");

        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format((double) size).concat(" Bytes");
        }
        if (isSpeed) return hrSize + "/s";
        return hrSize;
    }


    public static class JSONGetter {
        public static int getInt_or_Null(JSONObject object, String name) {
            Integer value = null;
            try {
                value = object.getInt(name);
            } catch (Exception e) {
            }
            return value;
        }

        public static String getString_or_Null(JSONObject object, String name) {
            String value = null;
            try {
                value = object.getString(name);
            } catch (Exception e) {
            }
            return value;
        }

        public static Boolean getBool_or_Null(JSONObject object, String name) {
            Boolean value = null;
            try {
                value = object.getBoolean(name);
            } catch (Exception e) {
            }
            return value;
        }

        public static JSONObject getObj_or_Null(JSONObject object, String name) {
            JSONObject value = null;
            try {
                value = object.getJSONObject(name);
            } catch (Exception e) {
            }
            return value;
        }

        public static JSONObject getObj_or_Null(JSONArray array, int index) {
            JSONObject value = null;
            try {
                value = array.getJSONObject(0);
            } catch (Exception e) {
            }
            return value;
        }

        public static JSONArray getArray_or_Null(JSONObject object, String name) {
            JSONArray value = null;
            try {
                value = object.getJSONArray(name);
            } catch (Exception e) {
            }
            return value;
        }

    }


}