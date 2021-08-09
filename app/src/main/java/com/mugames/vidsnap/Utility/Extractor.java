package com.mugames.vidsnap.Utility;

import com.mugames.vidsnap.Utility.UtilityInterface.AnalyzeCallback;

public abstract class Extractor {
    public abstract void Analyze(String url, AnalyzeCallback analyzeCallback);
}
