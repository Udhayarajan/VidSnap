package com.mugames.vidsnap.Utility;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Formats {
    public String title;
    public Bitmap thumbNailBit;
    public ArrayList<String> videoURLs;
    public ArrayList<String> qualities;
    public ArrayList<String> mimeTypes_video;
    public ArrayList<String> mimeTypes_audio;
    public ArrayList<String> audioURLs;
    public ArrayList<String> raw_quality_size;
    public ArrayList<String> quality_size;
    public String thumbNailURL;
    public String src;

    //used only by Instagram.java
    public ArrayList<String> thumbNailsURL;
    public ArrayList<Bitmap> thumbNailsBitMap;

    // Used only by YouTube.java
    public String audioSIG;
    public String audioSP;
    public ArrayList<String> videoSIGs;
    public ArrayList<String> videoSPs;//to find that link needed to be generate should have sp or sig
    public String audioURL;
    public String mimeType_audio;

    public Formats() {
        videoURLs =new ArrayList<>();
        qualities = new ArrayList<>();
        mimeTypes_video =new ArrayList<>();
        mimeTypes_audio =new ArrayList<>();
        audioURLs =new ArrayList<>();
        raw_quality_size =new ArrayList<>();
        quality_size =new ArrayList<>();
        videoSPs =new ArrayList<>();
        videoSIGs =new ArrayList<>();
        thumbNailsURL = new ArrayList<>();
        thumbNailsBitMap = new ArrayList<>();
    }
}