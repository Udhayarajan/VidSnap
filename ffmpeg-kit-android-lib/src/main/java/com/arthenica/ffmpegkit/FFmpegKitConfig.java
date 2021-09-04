/*
 * Copyright (c) 2018-2021 Taner Sener
 *
 * This file is part of FFmpegKit.
 *
 * FFmpegKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FFmpegKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with FFmpegKit.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.arthenica.ffmpegkit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.SparseArray;

import com.arthenica.smartexception.java.Exceptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Configuration class of <code>FFmpegKit</code> library.
 */
public class FFmpegKitConfig {

    /**
     * The tag used for logging.
     */
    static final String TAG = "ffmpeg-kit";

    /**
     * Prefix of named pipes created by ffmpeg kit.
     */
    static final String FFMPEG_KIT_NAMED_PIPE_PREFIX = "fk_pipe_";

    /**
     * Generates ids for named ffmpeg kit pipes.
     */
    private static final AtomicLong pipeIndexGenerator;

    private static Level activeLogLevel;

    /* Session history variables */
    private static int sessionHistorySize;
    private static final Map<Long, Session> sessionHistoryMap;
    private static final List<Session> sessionHistoryList;
    private static final Object sessionHistoryLock;

    private static int asyncConcurrencyLimit;
    private static ExecutorService asyncExecutorService;

    /* Global callbacks */
    private static LogCallback globalLogCallbackFunction;
    private static StatisticsCallback globalStatisticsCallbackFunction;
    private static ExecuteCallback globalExecuteCallbackFunction;
    private static final SparseArray<ParcelFileDescriptor> pfdMap;
    private static LogRedirectionStrategy globalLogRedirectionStrategy;

    static {

        Exceptions.registerRootPackage("com.arthenica");

        android.util.Log.i(FFmpegKitConfig.TAG, "Loading ffmpeg-kit.");

        final boolean nativeFFmpegTriedAndFailed = NativeLoader.loadFFmpeg();

        /* ALL FFMPEG-KIT LIBRARIES LOADED AT STARTUP */
        Abi.class.getName();
        FFmpegKit.class.getName();
        FFprobeKit.class.getName();

        NativeLoader.loadFFmpegKit(nativeFFmpegTriedAndFailed);

        android.util.Log.i(FFmpegKitConfig.TAG, String.format("Loaded ffmpeg-kit-%s-%s-%s-%s.", NativeLoader.loadPackageName(), NativeLoader.loadAbi(), NativeLoader.loadVersion(), NativeLoader.loadBuildDate()));

        pipeIndexGenerator = new AtomicLong(1);

        /* NATIVE LOG LEVEL IS RECEIVED ONLY ON STARTUP */
        activeLogLevel = Level.from(NativeLoader.loadLogLevel());

        asyncConcurrencyLimit = 10;
        asyncExecutorService = Executors.newFixedThreadPool(asyncConcurrencyLimit);

        sessionHistorySize = 10;
        sessionHistoryMap = new LinkedHashMap<Long, Session>() {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Session> eldest) {
                return (this.size() > sessionHistorySize);
            }
        };
        sessionHistoryList = new LinkedList<>();
        sessionHistoryLock = new Object();

        globalLogCallbackFunction = null;
        globalStatisticsCallbackFunction = null;
        globalExecuteCallbackFunction = null;

        pfdMap = new SparseArray<>();
        globalLogRedirectionStrategy = LogRedirectionStrategy.PRINT_LOGS_WHEN_NO_CALLBACKS_DEFINED;

        NativeLoader.enableRedirection();
    }

    /**
     * Default constructor hidden.
     */
    private FFmpegKitConfig() {
    }

    /**
     * <p>Enables log and statistics redirection.
     *
     * <p>When redirection is enabled FFmpeg/FFprobe logs are redirected to Logcat and sessions
     * collect log and statistics entries for the executions. It is possible to define global or
     * session specific log/statistics callbacks as well.
     *
     * <p>Note that redirection is enabled by default. If you do not want to use its functionality
     * please use {@link #disableRedirection()} to disable it.
     */
    public static void enableRedirection() {
        enableNativeRedirection();
    }

    /**
     * <p>Disables log and statistics redirection.
     *
     * <p>When redirection is disabled logs are printed to stderr, all logs and statistics
     * callbacks are disabled and <code>FFprobe</code>'s <code>getMediaInformation</code> methods
     * do not work.
     */
    public static void disableRedirection() {
        disableNativeRedirection();
    }

    /**
     * <p>Log redirection method called by the native library.
     *
     * @param sessionId  id of the session that generated this log, 0 for logs that do not belong
     *                   to a specific session
     * @param levelValue log level as defined in {@link Level}
     * @param logMessage redirected log message data
     */
    private static void log(final long sessionId, final int levelValue, final byte[] logMessage) {
        final Level level = Level.from(levelValue);
        final String text = new String(logMessage);
        final Log log = new Log(sessionId, level, text);
        boolean globalCallbackDefined = false;
        boolean sessionCallbackDefined = false;
        LogRedirectionStrategy activeLogRedirectionStrategy = globalLogRedirectionStrategy;

        // AV_LOG_STDERR logs are always redirected
        if ((activeLogLevel == Level.AV_LOG_QUIET && levelValue != Level.AV_LOG_STDERR.getValue()) || levelValue > activeLogLevel.getValue()) {
            // LOG NEITHER PRINTED NOR FORWARDED
            return;
        }

        final Session session = getSession(sessionId);
        if (session != null) {
            activeLogRedirectionStrategy = session.getLogRedirectionStrategy();
            session.addLog(log);

            if (session.getLogCallback() != null) {
                sessionCallbackDefined = true;

                try {
                    // NOTIFY SESSION CALLBACK DEFINED
                    session.getLogCallback().apply(log);
                } catch (final Exception e) {
                    android.util.Log.e(FFmpegKitConfig.TAG, String.format("Exception thrown inside session LogCallback block.%s", Exceptions.getStackTraceString(e)));
                }
            }
        }

        final LogCallback globalLogCallbackFunction = FFmpegKitConfig.globalLogCallbackFunction;
        if (globalLogCallbackFunction != null) {
            globalCallbackDefined = true;

            try {
                // NOTIFY GLOBAL CALLBACK DEFINED
                globalLogCallbackFunction.apply(log);
            } catch (final Exception e) {
                android.util.Log.e(FFmpegKitConfig.TAG, String.format("Exception thrown inside global LogCallback block.%s", Exceptions.getStackTraceString(e)));
            }
        }

        // EXECUTE THE LOG STRATEGY
        switch (activeLogRedirectionStrategy) {
            case NEVER_PRINT_LOGS: {
                return;
            }
            case PRINT_LOGS_WHEN_GLOBAL_CALLBACK_NOT_DEFINED: {
                if (globalCallbackDefined) {
                    return;
                }
            }
            break;
            case PRINT_LOGS_WHEN_SESSION_CALLBACK_NOT_DEFINED: {
                if (sessionCallbackDefined) {
                    return;
                }
            }
            case PRINT_LOGS_WHEN_NO_CALLBACKS_DEFINED: {
                if (globalCallbackDefined || sessionCallbackDefined) {
                    return;
                }
            }
            case ALWAYS_PRINT_LOGS: {
            }
        }

        // PRINT LOGS
        switch (level) {
            case AV_LOG_QUIET: {
                // PRINT NO OUTPUT
            }
            break;
            case AV_LOG_TRACE:
            case AV_LOG_DEBUG: {
                android.util.Log.d(TAG, text);
            }
            break;
            case AV_LOG_INFO: {
                android.util.Log.i(TAG, text);
            }
            break;
            case AV_LOG_WARNING: {
                android.util.Log.w(TAG, text);
            }
            break;
            case AV_LOG_ERROR:
            case AV_LOG_FATAL:
            case AV_LOG_PANIC: {
                android.util.Log.e(TAG, text);
            }
            break;
            case AV_LOG_STDERR:
            case AV_LOG_VERBOSE:
            default: {
                android.util.Log.v(TAG, text);
            }
            break;
        }
    }

    /**
     * <p>Statistics redirection method called by the native library.
     *
     * @param sessionId        id of the session that generated this statistics, 0 by default
     * @param videoFrameNumber frame number for videos
     * @param videoFps         frames per second value for videos
     * @param videoQuality     quality of the video stream
     * @param size             size in bytes
     * @param time             processed duration in milliseconds
     * @param bitrate          output bit rate in kbits/s
     * @param speed            processing speed = processed duration / operation duration
     */
    private static void statistics(final long sessionId, final int videoFrameNumber,
                                   final float videoFps, final float videoQuality, final long size,
                                   final int time, final double bitrate, final double speed) {
        final Statistics statistics = new Statistics(sessionId, videoFrameNumber, videoFps, videoQuality, size, time, bitrate, speed);

        final Session session = getSession(sessionId);
        if (session != null && session.isFFmpeg()) {
            FFmpegSession ffmpegSession = (FFmpegSession) session;
            ffmpegSession.addStatistics(statistics);

            if (ffmpegSession.getStatisticsCallback() != null) {
                try {
                    // NOTIFY SESSION CALLBACK IF DEFINED
                    ffmpegSession.getStatisticsCallback().apply(statistics);
                } catch (final Exception e) {
                    android.util.Log.e(FFmpegKitConfig.TAG, String.format("Exception thrown inside session StatisticsCallback block.%s", Exceptions.getStackTraceString(e)));
                }
            }
        }

        final StatisticsCallback globalStatisticsCallbackFunction = FFmpegKitConfig.globalStatisticsCallbackFunction;
        if (globalStatisticsCallbackFunction != null) {
            try {
                // NOTIFY GLOBAL CALLBACK IF DEFINED
                globalStatisticsCallbackFunction.apply(statistics);
            } catch (final Exception e) {
                android.util.Log.e(FFmpegKitConfig.TAG, String.format("Exception thrown inside global StatisticsCallback block.%s", Exceptions.getStackTraceString(e)));
            }
        }
    }

    /**
     * <p>Sets and overrides <code>fontconfig</code> configuration directory.
     *
     * @param path directory that contains fontconfig configuration (fonts.conf)
     * @return zero on success, non-zero on error
     */
    public static int setFontconfigConfigurationPath(final String path) {
        return setNativeEnvironmentVariable("FONTCONFIG_PATH", path);
    }

    /**
     * <p>Registers the fonts inside the given path, so they become available to use in FFmpeg
     * filters.
     *
     * <p>Note that you need to build <code>FFmpegKit</code> with <code>fontconfig</code>
     * enabled or use a prebuilt package with <code>fontconfig</code> inside to be able to use
     * fonts in <code>FFmpeg</code>.
     *
     * @param context           application context to access application data
     * @param fontDirectoryPath directory that contains fonts (.ttf and .otf files)
     * @param fontNameMapping   custom font name mappings, useful to access your fonts with more
     *                          friendly names
     */
    public static void setFontDirectory(final Context context, final String fontDirectoryPath, final Map<String, String> fontNameMapping) {
        setFontDirectoryList(context, Collections.singletonList(fontDirectoryPath), fontNameMapping);
    }

    /**
     * <p>Registers the fonts inside the given list of font directories, so they become available
     * to use in FFmpeg filters.
     *
     * <p>Note that you need to build <code>FFmpegKit</code> with <code>fontconfig</code>
     * enabled or use a prebuilt package with <code>fontconfig</code> inside to be able to use
     * fonts in <code>FFmpeg</code>.
     *
     * @param context           application context to access application data
     * @param fontDirectoryList list of directories that contain fonts (.ttf and .otf files)
     * @param fontNameMapping   custom font name mappings, useful to access your fonts with more
     *                          friendly names
     */
    public static void setFontDirectoryList(final Context context, final List<String> fontDirectoryList, final Map<String, String> fontNameMapping) {
        final File cacheDir = context.getCacheDir();
        int validFontNameMappingCount = 0;

        final File tempConfigurationDirectory = new File(cacheDir, "fontconfig");
        if (!tempConfigurationDirectory.exists()) {
            boolean tempFontConfDirectoryCreated = tempConfigurationDirectory.mkdirs();
            android.util.Log.d(TAG, String.format("Created temporary font conf directory: %s.", tempFontConfDirectoryCreated));
        }

        final File fontConfiguration = new File(tempConfigurationDirectory, "fonts.conf");
        if (fontConfiguration.exists()) {
            boolean fontConfigurationDeleted = fontConfiguration.delete();
            android.util.Log.d(TAG, String.format("Deleted old temporary font configuration: %s.", fontConfigurationDeleted));
        }

        /* PROCESS MAPPINGS FIRST */
        final StringBuilder fontNameMappingBlock = new StringBuilder("");
        if (fontNameMapping != null && (fontNameMapping.size() > 0)) {
            fontNameMapping.entrySet();
            for (Map.Entry<String, String> mapping : fontNameMapping.entrySet()) {
                String fontName = mapping.getKey();
                String mappedFontName = mapping.getValue();

                if ((fontName != null) && (mappedFontName != null) && (fontName.trim().length() > 0) && (mappedFontName.trim().length() > 0)) {
                    fontNameMappingBlock.append("        <match target=\"pattern\">\n");
                    fontNameMappingBlock.append("                <test qual=\"any\" name=\"family\">\n");
                    fontNameMappingBlock.append(String.format("                        <string>%s</string>\n", fontName));
                    fontNameMappingBlock.append("                </test>\n");
                    fontNameMappingBlock.append("                <edit name=\"family\" mode=\"assign\" binding=\"same\">\n");
                    fontNameMappingBlock.append(String.format("                        <string>%s</string>\n", mappedFontName));
                    fontNameMappingBlock.append("                </edit>\n");
                    fontNameMappingBlock.append("        </match>\n");

                    validFontNameMappingCount++;
                }
            }
        }

        final StringBuilder fontConfigBuilder = new StringBuilder();
        fontConfigBuilder.append("<?xml version=\"1.0\"?>\n");
        fontConfigBuilder.append("<!DOCTYPE fontconfig SYSTEM \"fonts.dtd\">\n");
        fontConfigBuilder.append("<fontconfig>\n");
        fontConfigBuilder.append("    <dir prefix=\"cwd\">.</dir>\n");
        for (String fontDirectoryPath : fontDirectoryList) {
            fontConfigBuilder.append("    <dir>");
            fontConfigBuilder.append(fontDirectoryPath);
            fontConfigBuilder.append("</dir>\n");
        }
        fontConfigBuilder.append(fontNameMappingBlock);
        fontConfigBuilder.append("</fontconfig>");

        final AtomicReference<FileOutputStream> reference = new AtomicReference<>();
        try {
            final FileOutputStream outputStream = new FileOutputStream(fontConfiguration);
            reference.set(outputStream);

            outputStream.write(fontConfigBuilder.toString().getBytes());
            outputStream.flush();

            android.util.Log.d(TAG, String.format("Saved new temporary font configuration with %d font name mappings.", validFontNameMappingCount));

            setFontconfigConfigurationPath(tempConfigurationDirectory.getAbsolutePath());

            for (String fontDirectoryPath : fontDirectoryList) {
                android.util.Log.d(TAG, String.format("Font directory %s registered successfully.", fontDirectoryPath));
            }

        } catch (final IOException e) {
            android.util.Log.e(TAG, String.format("Failed to set font directory: %s.%s", Arrays.toString(fontDirectoryList.toArray()), Exceptions.getStackTraceString(e)));
        } finally {
            if (reference.get() != null) {
                try {
                    reference.get().close();
                } catch (IOException e) {
                    // DO NOT PRINT THIS ERROR
                }
            }
        }
    }

    /**
     * <p>Creates a new named pipe to use in <code>FFmpeg</code> operations.
     *
     * <p>Please note that creator is responsible of closing created pipes.
     *
     * @param context application context
     * @return the full path of the named pipe
     */
    public static String registerNewFFmpegPipe(final Context context) {

        // PIPES ARE CREATED UNDER THE PIPES DIRECTORY
        final File cacheDir = context.getCacheDir();
        final File pipesDir = new File(cacheDir, "pipes");

        if (!pipesDir.exists()) {
            final boolean pipesDirCreated = pipesDir.mkdirs();
            if (!pipesDirCreated) {
                android.util.Log.e(TAG, String.format("Failed to create pipes directory: %s.", pipesDir.getAbsolutePath()));
                return null;
            }
        }

        final String newFFmpegPipePath = MessageFormat.format("{0}{1}{2}{3}", pipesDir, File.separator, FFMPEG_KIT_NAMED_PIPE_PREFIX, pipeIndexGenerator.getAndIncrement());

        // FIRST CLOSE OLD PIPES WITH THE SAME NAME
        closeFFmpegPipe(newFFmpegPipePath);

        int rc = registerNewNativeFFmpegPipe(newFFmpegPipePath);
        if (rc == 0) {
            return newFFmpegPipePath;
        } else {
            android.util.Log.e(TAG, String.format("Failed to register new FFmpeg pipe %s. Operation failed with rc=%d.", newFFmpegPipePath, rc));
            return null;
        }
    }

    /**
     * <p>Closes a previously created <code>FFmpeg</code> pipe.
     *
     * @param ffmpegPipePath full path of the FFmpeg pipe
     */
    public static void closeFFmpegPipe(final String ffmpegPipePath) {
        final File file = new File(ffmpegPipePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Returns the list of camera ids supported. These devices can be used in <code>FFmpeg</code>
     * commands.
     *
     * <p>Note that this method requires API Level &ge; 24. On older API levels it returns an empty
     * list.
     *
     * @param context application context
     * @return list of camera ids supported or an empty list if no supported cameras are found
     */
    public static List<String> getSupportedCameraIds(final Context context) {
        final List<String> detectedCameraIdList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            detectedCameraIdList.addAll(CameraSupport.extractSupportedCameraIds(context));
        }

        return detectedCameraIdList;
    }

    /**
     * <p>Returns the version of FFmpeg bundled within <code>FFmpegKit</code> library.
     *
     * @return the version of FFmpeg
     */
    public static String getFFmpegVersion() {
        return getNativeFFmpegVersion();
    }

    /**
     * <p>Returns FFmpegKit library version.
     *
     * @return FFmpegKit version
     */
    public static String getVersion() {
        if (isLTSBuild()) {
            return String.format("%s-lts", getNativeVersion());
        } else {
            return getNativeVersion();
        }
    }

    /**
     * <p>Returns whether FFmpegKit release is a Long Term Release or not.
     *
     * @return true/yes or false/no
     */
    public static boolean isLTSBuild() {
        return AbiDetect.isNativeLTSBuild();
    }

    /**
     * <p>Returns FFmpegKit library build date.
     *
     * @return FFmpegKit library build date
     */
    public static String getBuildDate() {
        return getNativeBuildDate();
    }

    /**
     * <p>Prints the given string to Logcat using the given priority. If string provided is bigger
     * than the Logcat buffer, the string is printed in multiple lines.
     *
     * @param logPriority one of {@link android.util.Log#VERBOSE},
     *                    {@link android.util.Log#DEBUG},
     *                    {@link android.util.Log#INFO},
     *                    {@link android.util.Log#WARN},
     *                    {@link android.util.Log#ERROR},
     *                    {@link android.util.Log#ASSERT}
     * @param string      string to be printed
     */
    public static void printToLogcat(final int logPriority, final String string) {
        final int LOGGER_ENTRY_MAX_LEN = 4 * 1000;

        String remainingString = string;
        do {
            if (remainingString.length() <= LOGGER_ENTRY_MAX_LEN) {
                android.util.Log.println(logPriority, FFmpegKitConfig.TAG, remainingString);
                remainingString = "";
            } else {
                final int index = remainingString.substring(0, LOGGER_ENTRY_MAX_LEN).lastIndexOf('\n');
                if (index < 0) {
                    android.util.Log.println(logPriority, FFmpegKitConfig.TAG, remainingString.substring(0, LOGGER_ENTRY_MAX_LEN));
                    remainingString = remainingString.substring(LOGGER_ENTRY_MAX_LEN);
                } else {
                    android.util.Log.println(logPriority, FFmpegKitConfig.TAG, remainingString.substring(0, index));
                    remainingString = remainingString.substring(index);
                }
            }
        } while (remainingString.length() > 0);
    }

    /**
     * <p>Sets an environment variable.
     *
     * @param variableName  environment variable name
     * @param variableValue environment variable value
     * @return zero on success, non-zero on error
     */
    public static int setEnvironmentVariable(final String variableName, final String variableValue) {
        return setNativeEnvironmentVariable(variableName, variableValue);
    }

    /**
     * <p>Registers a new ignored signal. Ignored signals are not handled by <code>FFmpegKit</code>
     * library.
     *
     * @param signal signal to be ignored
     */
    public static void ignoreSignal(final Signal signal) {
        ignoreNativeSignal(signal.getValue());
    }

    /**
     * <p>Synchronously executes the FFmpeg session provided.
     *
     * @param ffmpegSession FFmpeg session which includes command options/arguments
     */
    public static void ffmpegExecute(final FFmpegSession ffmpegSession) {
        addSession(ffmpegSession);
        ffmpegSession.startRunning();

        try {
            final int returnCode = nativeFFmpegExecute(ffmpegSession.getSessionId(), ffmpegSession.getArguments());
            ffmpegSession.complete(new ReturnCode(returnCode));
        } catch (final Exception e) {
            ffmpegSession.fail(e);
            android.util.Log.w(FFmpegKitConfig.TAG, String.format("FFmpeg execute failed: %s.%s", FFmpegKit.argumentsToString(ffmpegSession.getArguments()), Exceptions.getStackTraceString(e)));
        }
    }

    /**
     * <p>Synchronously executes the FFprobe session provided.
     *
     * @param ffprobeSession FFprobe session which includes command options/arguments
     */
    public static void ffprobeExecute(final FFprobeSession ffprobeSession) {
        addSession(ffprobeSession);
        ffprobeSession.startRunning();

        try {
            final int returnCode = nativeFFprobeExecute(ffprobeSession.getSessionId(), ffprobeSession.getArguments());
            ffprobeSession.complete(new ReturnCode(returnCode));
        } catch (final Exception e) {
            ffprobeSession.fail(e);
            android.util.Log.w(FFmpegKitConfig.TAG, String.format("FFprobe execute failed: %s.%s", FFmpegKit.argumentsToString(ffprobeSession.getArguments()), Exceptions.getStackTraceString(e)));
        }
    }

    /**
     * <p>Synchronously executes the media information session provided.
     *
     * @param mediaInformationSession media information session which includes command options/arguments
     * @param waitTimeout             max time to wait until media information is transmitted
     */
    public static void getMediaInformationExecute(final MediaInformationSession mediaInformationSession, final int waitTimeout) {
        addSession(mediaInformationSession);
        mediaInformationSession.startRunning();

        try {
            final int returnCodeValue = nativeFFprobeExecute(mediaInformationSession.getSessionId(), mediaInformationSession.getArguments());
            final ReturnCode returnCode = new ReturnCode(returnCodeValue);
            mediaInformationSession.complete(returnCode);
            if (returnCode.isSuccess()) {
                MediaInformation mediaInformation = MediaInformationJsonParser.fromWithError(mediaInformationSession.getAllLogsAsString(waitTimeout));
                mediaInformationSession.setMediaInformation(mediaInformation);
            }
        } catch (final Exception e) {
            mediaInformationSession.fail(e);
            android.util.Log.w(FFmpegKitConfig.TAG, String.format("Get media information execute failed: %s.%s", FFmpegKit.argumentsToString(mediaInformationSession.getArguments()), Exceptions.getStackTraceString(e)));
        }
    }

    /**
     * <p>Asynchronously executes the FFmpeg session provided.
     *
     * @param ffmpegSession FFmpeg session which includes command options/arguments
     */
    public static void asyncFFmpegExecute(final FFmpegSession ffmpegSession) {
        addSession(ffmpegSession);

        AsyncFFmpegExecuteTask asyncFFmpegExecuteTask = new AsyncFFmpegExecuteTask(ffmpegSession);
        Future<?> future = asyncExecutorService.submit(asyncFFmpegExecuteTask);
        ffmpegSession.setFuture(future);
    }

    /**
     * <p>Asynchronously executes the FFmpeg session provided.
     *
     * @param ffmpegSession   FFmpeg session which includes command options/arguments
     * @param executorService executor service that will be used to run this asynchronous operation
     */
    public static void asyncFFmpegExecute(final FFmpegSession ffmpegSession, final ExecutorService executorService) {
        addSession(ffmpegSession);

        AsyncFFmpegExecuteTask asyncFFmpegExecuteTask = new AsyncFFmpegExecuteTask(ffmpegSession);
        Future<?> future = executorService.submit(asyncFFmpegExecuteTask);
        ffmpegSession.setFuture(future);
    }

    /**
     * <p>Asynchronously executes the FFprobe session provided.
     *
     * @param ffprobeSession FFprobe session which includes command options/arguments
     */
    public static void asyncFFprobeExecute(final FFprobeSession ffprobeSession) {
        addSession(ffprobeSession);

        AsyncFFprobeExecuteTask asyncFFmpegExecuteTask = new AsyncFFprobeExecuteTask(ffprobeSession);
        Future<?> future = asyncExecutorService.submit(asyncFFmpegExecuteTask);
        ffprobeSession.setFuture(future);
    }

    /**
     * <p>Asynchronously executes the FFprobe session provided.
     *
     * @param ffprobeSession  FFprobe session which includes command options/arguments
     * @param executorService executor service that will be used to run this asynchronous operation
     */
    public static void asyncFFprobeExecute(final FFprobeSession ffprobeSession, final ExecutorService executorService) {
        addSession(ffprobeSession);

        AsyncFFprobeExecuteTask asyncFFmpegExecuteTask = new AsyncFFprobeExecuteTask(ffprobeSession);
        Future<?> future = executorService.submit(asyncFFmpegExecuteTask);
        ffprobeSession.setFuture(future);
    }

    /**
     * <p>Asynchronously executes the media information session provided.
     *
     * @param mediaInformationSession media information session which includes command options/arguments
     * @param waitTimeout             max time to wait until media information is transmitted
     */
    public static void asyncGetMediaInformationExecute(final MediaInformationSession mediaInformationSession, final int waitTimeout) {
        addSession(mediaInformationSession);

        AsyncGetMediaInformationTask asyncGetMediaInformationTask = new AsyncGetMediaInformationTask(mediaInformationSession, waitTimeout);
        Future<?> future = asyncExecutorService.submit(asyncGetMediaInformationTask);
        mediaInformationSession.setFuture(future);
    }

    /**
     * <p>Asynchronously executes the media information session provided.
     *
     * @param mediaInformationSession media information session which includes command options/arguments
     * @param executorService         executor service that will be used to run this asynchronous operation
     * @param waitTimeout             max time to wait until media information is transmitted
     */
    public static void asyncGetMediaInformationExecute(final MediaInformationSession mediaInformationSession, final ExecutorService executorService, final int waitTimeout) {
        addSession(mediaInformationSession);

        AsyncGetMediaInformationTask asyncGetMediaInformationTask = new AsyncGetMediaInformationTask(mediaInformationSession, waitTimeout);
        Future<?> future = executorService.submit(asyncGetMediaInformationTask);
        mediaInformationSession.setFuture(future);
    }

    /**
     * Returns the maximum number of async sessions that will be executed in parallel.
     *
     * @return maximum number of async sessions that will be executed in parallel
     */
    public static int getAsyncConcurrencyLimit() {
        return asyncConcurrencyLimit;
    }

    /**
     * Sets the maximum number of async sessions that will be executed in parallel. If more
     * sessions are submitted those will be queued.
     *
     * @param asyncConcurrencyLimit new async concurrency limit
     */
    public static void setAsyncConcurrencyLimit(final int asyncConcurrencyLimit) {

        if (asyncConcurrencyLimit > 0) {

            /* SET THE NEW LIMIT */
            FFmpegKitConfig.asyncConcurrencyLimit = asyncConcurrencyLimit;
            ExecutorService oldAsyncExecutorService = FFmpegKitConfig.asyncExecutorService;

            /* CREATE THE NEW ASYNC THREAD POOL */
            FFmpegKitConfig.asyncExecutorService = Executors.newFixedThreadPool(asyncConcurrencyLimit);

            /* STOP THE OLD ASYNC THREAD POOL */
            oldAsyncExecutorService.shutdown();
        }
    }

    /**
     * <p>Sets a global callback function to redirect FFmpeg/FFprobe logs.
     *
     * @param logCallback log callback function or null to disable a previously defined
     *                    callback
     */
    public static void enableLogCallback(final LogCallback logCallback) {
        globalLogCallbackFunction = logCallback;
    }

    /**
     * <p>Sets a global callback function to redirect FFmpeg statistics.
     *
     * @param statisticsCallback statistics callback function or null to disable a previously
     *                           defined callback
     */
    public static void enableStatisticsCallback(final StatisticsCallback statisticsCallback) {
        globalStatisticsCallbackFunction = statisticsCallback;
    }

    /**
     * <p>Sets a global callback function to receive execution results.
     *
     * @param executeCallback execute callback function or null to disable a previously
     *                        defined callback
     */
    public static void enableExecuteCallback(final ExecuteCallback executeCallback) {
        globalExecuteCallbackFunction = executeCallback;
    }

    /**
     * <p>Returns the global execute callback function.
     *
     * @return global execute callback function
     */
    static ExecuteCallback getExecuteCallback() {
        return globalExecuteCallbackFunction;
    }

    /**
     * Returns the current log level.
     *
     * @return current log level
     */
    public static Level getLogLevel() {
        return activeLogLevel;
    }

    /**
     * Sets the log level.
     *
     * @param level new log level
     */
    public static void setLogLevel(final Level level) {
        if (level != null) {
            activeLogLevel = level;
            setNativeLogLevel(level.getValue());
        }
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * input/output url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level >= 19. On older API levels it returns an empty url.
     *
     * @return input/output url that can be passed to FFmpegKit or FFprobeKit
     */
    private static String getSafParameter(final Context context, final Uri uri, final String openMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            android.util.Log.i(TAG, String.format("getSafParameter is not supported on API Level %d", Build.VERSION.SDK_INT));
            return "";
        }

        String displayName = "unknown";
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);// These line is modified because of Error : Value must be >= 0
                displayName = cursor.getString(index);
            }
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to get %s column for %s.%s", DocumentsContract.Document.COLUMN_DISPLAY_NAME, uri.toString(), Exceptions.getStackTraceString(t)));
        }

        int fd = -1;
        try {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, openMode);
            fd = parcelFileDescriptor.getFd();
            pfdMap.put(fd, parcelFileDescriptor);
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to obtain %s parcelFileDescriptor for %s.%s", openMode, uri.toString(), Exceptions.getStackTraceString(t)));
        }

        // workaround for https://issuetracker.google.com/issues/162440528: ANDROID_CREATE_DOCUMENT generating file names like "transcode.mp3 (2)"
        if (displayName.lastIndexOf('.') > 0 && displayName.lastIndexOf(' ') > displayName.lastIndexOf('.')) {
            String extension = displayName.substring(displayName.lastIndexOf('.'), displayName.lastIndexOf(' '));
            displayName += extension;
        }
        // spaces can break argument list parsing, see https://github.com/alexcohn/mobile-ffmpeg/pull/1#issuecomment-688643836
        final char NBSP = (char) 0xa0;
        return "saf:" + fd + "/" + displayName.replace(' ', NBSP);
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * input url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri saf uri
     * @return input url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForRead(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "r");
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * output url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri saf uri
     * @return output url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForWrite(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "w");
    }

    /**
     * Called by saf_wrapper from native library to close a parcel file descriptor.
     *
     * @param fd parcel file descriptor created for a saf uri
     */
    private static void closeParcelFileDescriptor(final int fd) {
        try {
            ParcelFileDescriptor pfd = pfdMap.get(fd);
            if (pfd != null) {
                pfd.close();
                pfdMap.delete(fd);
            }
        } catch (final Throwable t) {
            android.util.Log.e(TAG, String.format("Failed to close file descriptor: %d.%s", fd, Exceptions.getStackTraceString(t)));
        }
    }

    /**
     * Returns the session history size.
     *
     * @return session history size
     */
    public static int getSessionHistorySize() {
        return sessionHistorySize;
    }

    /**
     * Sets the session history size.
     *
     * @param sessionHistorySize session history size, should be smaller than 1000
     */
    public static void setSessionHistorySize(final int sessionHistorySize) {
        if (sessionHistorySize >= 1000) {

            /*
             * THERE IS A HARD LIMIT ON THE NATIVE SIDE. HISTORY SIZE MUST BE SMALLER THAN 1000
             */
            throw new IllegalArgumentException("Session history size must not exceed the hard limit!");
        } else if (sessionHistorySize > 0) {
            FFmpegKitConfig.sessionHistorySize = sessionHistorySize;
        }
    }

    /**
     * Adds a session to the session history.
     *
     * @param session new session
     */
    static void addSession(final Session session) {
        synchronized (sessionHistoryLock) {

            /*
             * ASYNC SESSIONS CALL THIS METHOD TWICE
             * THIS CHECK PREVENTS ADDING THE SAME SESSION TWICE
             */
            final boolean sessionAlreadyAdded = sessionHistoryMap.containsKey(session.getSessionId());
            if (!sessionAlreadyAdded) {
                sessionHistoryMap.put(session.getSessionId(), session);
                sessionHistoryList.add(session);
                if (sessionHistoryList.size() > sessionHistorySize) {
                    try {
                        sessionHistoryList.remove(0);
                    } catch (final IndexOutOfBoundsException ignored) {
                    }
                }
            }
        }
    }

    /**
     * Returns the session specified with <code>sessionId</code> from the session history.
     *
     * @param sessionId session identifier
     * @return session specified with sessionId or null if it is not found in the history
     */
    public static Session getSession(final long sessionId) {
        synchronized (sessionHistoryLock) {
            return sessionHistoryMap.get(sessionId);
        }
    }

    /**
     * Returns the last session created from the session history.
     *
     * @return the last session created or null if session history is empty
     */
    public static Session getLastSession() {
        synchronized (sessionHistoryLock) {
            if (sessionHistoryList.size() > 0) {
                return sessionHistoryList.get(sessionHistoryList.size() - 1);
            }
        }

        return null;
    }

    /**
     * Returns the last session completed from the session history.
     *
     * @return the last session completed. If there are no completed sessions in the history this
     * method will return null
     */
    public static Session getLastCompletedSession() {
        synchronized (sessionHistoryLock) {
            for (int i = sessionHistoryList.size() - 1; i >= 0; i--) {
                final Session session = sessionHistoryList.get(i);
                if (session.getState() == SessionState.COMPLETED) {
                    return session;
                }
            }
        }

        return null;
    }

    /**
     * <p>Returns all sessions in the session history.
     *
     * @return all sessions in the session history
     */
    public static List<Session> getSessions() {
        synchronized (sessionHistoryLock) {
            return new LinkedList<>(sessionHistoryList);
        }
    }

    /**
     * <p>Returns all FFmpeg sessions in the session history.
     *
     * @return all FFmpeg sessions in the session history
     */
    static List<FFmpegSession> getFFmpegSessions() {
        final LinkedList<FFmpegSession> list = new LinkedList<>();

        synchronized (sessionHistoryLock) {
            for (Session session : sessionHistoryList) {
                if (session.isFFmpeg()) {
                    list.add((FFmpegSession) session);
                }
            }
        }

        return list;
    }

    /**
     * <p>Returns all FFprobe sessions in the session history.
     *
     * @return all FFprobe sessions in the session history
     */
    static List<FFprobeSession> getFFprobeSessions() {
        final LinkedList<FFprobeSession> list = new LinkedList<>();

        synchronized (sessionHistoryLock) {
            for (Session session : sessionHistoryList) {
                if (session.isFFprobe()) {
                    list.add((FFprobeSession) session);
                }
            }
        }

        return list;
    }

    /**
     * <p>Returns sessions that have the given state.
     *
     * @param state session state
     * @return sessions that have the given state from the session history
     */
    public static List<Session> getSessionsByState(final SessionState state) {
        final LinkedList<Session> list = new LinkedList<>();

        synchronized (sessionHistoryLock) {
            for (Session session : sessionHistoryList) {
                if (session.getState() == state) {
                    list.add(session);
                }
            }
        }

        return list;
    }

    /**
     * Returns the active log redirection strategy.
     *
     * @return log redirection strategy
     */
    public static LogRedirectionStrategy getLogRedirectionStrategy() {
        return globalLogRedirectionStrategy;
    }

    /**
     * <p>Sets the log redirection strategy
     *
     * @param logRedirectionStrategy log redirection strategy
     */
    public static void setLogRedirectionStrategy(final LogRedirectionStrategy logRedirectionStrategy) {
        FFmpegKitConfig.globalLogRedirectionStrategy = logRedirectionStrategy;
    }

    /**
     * <p>Enables redirection natively.
     */
    private static native void enableNativeRedirection();

    /**
     * <p>Disables redirection natively.
     */
    private static native void disableNativeRedirection();

    /**
     * Returns native log level.
     *
     * @return log level
     */
    static native int getNativeLogLevel();

    /**
     * Sets native log level
     *
     * @param level log level
     */
    private static native void setNativeLogLevel(int level);

    /**
     * <p>Returns FFmpeg version bundled within the library natively.
     *
     * @return FFmpeg version
     */
    private native static String getNativeFFmpegVersion();

    /**
     * <p>Returns FFmpegKit library version natively.
     *
     * @return FFmpegKit version
     */
    private native static String getNativeVersion();

    /**
     * <p>Synchronously executes FFmpeg natively.
     *
     * @param sessionId id of the session
     * @param arguments FFmpeg command options/arguments as string array
     * @return {@link ReturnCode#SUCCESS} on successful execution and {@link ReturnCode#CANCEL} on
     * user cancel. Other non-zero values are returned on error. Use {@link ReturnCode} class to
     * handle the value
     */
    private native static int nativeFFmpegExecute(final long sessionId, final String[] arguments);

    /**
     * <p>Synchronously executes FFprobe natively.
     *
     * @param sessionId id of the session
     * @param arguments FFprobe command options/arguments as string array
     * @return {@link ReturnCode#SUCCESS} on successful execution and {@link ReturnCode#CANCEL} on
     * user cancel. Other non-zero values are returned on error. Use {@link ReturnCode} class to
     * handle the value
     */
    native static int nativeFFprobeExecute(final long sessionId, final String[] arguments);

    /**
     * <p>Cancels an ongoing FFmpeg operation natively. This function does not wait for termination
     * to complete and returns immediately.
     *
     * @param sessionId id of the session
     */
    native static void nativeFFmpegCancel(final long sessionId);

    /**
     * <p>Returns the number of native messages that are not transmitted to the Java callbacks for
     * this session natively.
     *
     * @param sessionId id of the session
     * @return number of native messages that are not transmitted to the Java callbacks for
     * this session natively
     */
    public native static int messagesInTransmit(final long sessionId);

    /**
     * <p>Creates a new named pipe to use in <code>FFmpeg</code> operations natively.
     *
     * <p>Please note that creator is responsible of closing created pipes.
     *
     * @param ffmpegPipePath full path of ffmpeg pipe
     * @return zero on successful creation, non-zero on error
     */
    private native static int registerNewNativeFFmpegPipe(final String ffmpegPipePath);

    /**
     * <p>Returns FFmpegKit library build date natively.
     *
     * @return FFmpegKit library build date
     */
    private native static String getNativeBuildDate();

    /**
     * <p>Sets an environment variable natively.
     *
     * @param variableName  environment variable name
     * @param variableValue environment variable value
     * @return zero on success, non-zero on error
     */
    private native static int setNativeEnvironmentVariable(final String variableName, final String variableValue);

    /**
     * <p>Registers a new ignored signal natively. Ignored signals are not handled by
     * <code>FFmpegKit</code> library.
     *
     * @param signum signal number
     */
    private native static void ignoreNativeSignal(final int signum);

}
