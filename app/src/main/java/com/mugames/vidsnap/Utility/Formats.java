package com.mugames.vidsnap.Utility;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Formats {
    public String title;

    public ArrayList<String> videoURLs;
    public ArrayList<String> qualities;
    public ArrayList<String> videoMime;
    public ArrayList<String> audioMime;
    public ArrayList<String> audioURLs;
    public ArrayList<Long> audioSizes;
    public ArrayList<Long> videoSizes;
    public ArrayList<String> videoSizeInString;// In displayable format 14.8MB etc

    public String src;

    public ArrayList<ArrayList<String>> manifest;//Used in m3u8 logic

    //used only by Instagram.java
    public ArrayList<String> thumbNailsURL;
    public ArrayList<Bitmap> thumbNailsBitMap;

    // Used only by YouTube.java
    public String audioSIG;
    public String audioSP;
    public ArrayList<String> videoSIGs;
    public ArrayList<String> videoSPs;//to find that link needed to be generate should have sp or sig

    public Formats() {
        videoURLs =new ArrayList<>();
        qualities = new ArrayList<>();
        videoMime =new ArrayList<>();
        audioMime =new ArrayList<>();
        audioURLs =new ArrayList<>();
        audioSizes = new ArrayList<>();
        videoSizes =new ArrayList<>();
        videoSizeInString =new ArrayList<>();
        videoSPs =new ArrayList<>();
        videoSIGs =new ArrayList<>();
        thumbNailsURL = new ArrayList<>();
        thumbNailsBitMap = new ArrayList<>();
        manifest = new ArrayList<>();
    }
}