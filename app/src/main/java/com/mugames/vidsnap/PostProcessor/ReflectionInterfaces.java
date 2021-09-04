package com.mugames.vidsnap.PostProcessor;


import java.lang.reflect.Method;

public interface ReflectionInterfaces{

    interface FFMPEGCallback {
        void apply(Class<?> sessionKlass,Object instance, String outputPath);
    }

    interface StatisticsCallback{
        void apply(Class<?> statisticsKlass,Object instance);
    }

    interface SOLoadCallbacks{
        void onSOLoadingSuccess();
        void onSOLoadingFailed(Exception e);
    }
}