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
 * You should have received a copy of the GNU Lesser General Public License
 * along with FFmpegKit.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.arthenica.ffmpegkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * <p>Main class to run <code>FFmpeg</code> commands. Supports executing commands both
 * synchronously and asynchronously.
 * <pre>
 * FFmpegSession session = FFmpegKit.execute("-i file1.mp4 -c:v libxvid file1.avi");
 *
 * FFmpegSession asyncSession = FFmpegKit.executeAsync("-i file1.mp4 -c:v libxvid file1.avi", executeCallback);
 * </pre>
 * <p>Provides overloaded <code>execute</code> methods to define session specific callbacks.
 * <pre>
 * FFmpegSession asyncSession = FFmpegKit.executeAsync("-i file1.mp4 -c:v libxvid file1.avi", executeCallback, logCallback, statisticsCallback);
 * </pre>
 */
public class FFmpegKit {

    static {
        AbiDetect.class.getName();
        FFmpegKitConfig.class.getName();
    }

    /**
     * Default constructor hidden.
     */
    private FFmpegKit() {
    }

    /**
     * <p>Synchronously executes FFmpeg with arguments provided.
     *
     * @param arguments FFmpeg command options/arguments as string array
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession execute(final String[] arguments) {
        final FFmpegSession session = new FFmpegSession(arguments);

        FFmpegKitConfig.ffmpegExecute(session);

        return session;
    }

    /**
     * <p>Asynchronously executes FFmpeg with arguments provided.
     *
     * @param arguments       FFmpeg command options/arguments as string array
     * @param executeCallback callback that will be called when the execution is completed
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String[] arguments,
                                             final ExecuteCallback executeCallback) {
        final FFmpegSession session = new FFmpegSession(arguments, executeCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session);

        return session;
    }

    /**
     * <p>Asynchronously executes FFmpeg with arguments provided.
     *
     * @param arguments          FFmpeg command options/arguments as string array
     * @param executeCallback    callback that will be called when the execution is completed
     * @param logCallback        callback that will receive logs
     * @param statisticsCallback callback that will receive statistics
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String[] arguments,
                                             final ExecuteCallback executeCallback,
                                             final LogCallback logCallback,
                                             final StatisticsCallback statisticsCallback) {
        final FFmpegSession session = new FFmpegSession(arguments, executeCallback, logCallback, statisticsCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session);

        return session;
    }

    /**
     * <p>Asynchronously executes FFmpeg with arguments provided.
     *
     * @param arguments       FFmpeg command options/arguments as string array
     * @param executeCallback callback that will be called when the execution is completed
     * @param executorService executor service that will be used to run this asynchronous operation
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String[] arguments,
                                             final ExecuteCallback executeCallback,
                                             final ExecutorService executorService) {
        final FFmpegSession session = new FFmpegSession(arguments, executeCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session, executorService);

        return session;
    }

    /**
     * <p>Asynchronously executes FFmpeg with arguments provided.
     *
     * @param arguments          FFmpeg command options/arguments as string array
     * @param executeCallback    callback that will be called when the execution is completed
     * @param logCallback        callback that will receive logs
     * @param statisticsCallback callback that will receive statistics
     * @param executorService    executor service that will be used to run this asynchronous operation
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String[] arguments,
                                             final ExecuteCallback executeCallback,
                                             final LogCallback logCallback,
                                             final StatisticsCallback statisticsCallback,
                                             final ExecutorService executorService) {
        final FFmpegSession session = new FFmpegSession(arguments, executeCallback, logCallback, statisticsCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session, executorService);

        return session;
    }

    /**
     * <p>Synchronously executes FFmpeg command provided. Space character is used to split command
     * into arguments. You can use single or double quote characters to specify arguments inside
     * your command.
     *
     * @param command FFmpeg command
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession execute(final String command) {
        return execute(parseArguments(command));
    }

    /**
     * <p>Asynchronously executes FFmpeg command provided. Space character is used to split command
     * into arguments. You can use single or double quote characters to specify arguments inside
     * your command.
     *
     * @param command         FFmpeg command
     * @param executeCallback callback that will be called when the execution is completed
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String command,
                                             final ExecuteCallback executeCallback) {
        return executeAsync(parseArguments(command), executeCallback);
    }

    /**
     * <p>Asynchronously executes FFmpeg command provided. Space character is used to split command
     * into arguments. You can use single or double quote characters to specify arguments inside
     * your command.
     *
     * @param command            FFmpeg command
     * @param executeCallback    callback that will be called when the execution is completed
     * @param logCallback        callback that will receive logs
     * @param statisticsCallback callback that will receive statistics
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String command,
                                             final ExecuteCallback executeCallback,
                                             final LogCallback logCallback,
                                             final StatisticsCallback statisticsCallback) {
        return executeAsync(parseArguments(command), executeCallback, logCallback, statisticsCallback);
    }

    /**
     * <p>Asynchronously executes FFmpeg command provided. Space character is used to split command
     * into arguments. You can use single or double quote characters to specify arguments inside
     * your command.
     *
     * @param command         FFmpeg command
     * @param executeCallback callback that will be called when the execution is completed
     * @param executorService executor service that will be used to run this asynchronous operation
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String command,
                                             final ExecuteCallback executeCallback,
                                             final ExecutorService executorService) {
        final FFmpegSession session = new FFmpegSession(parseArguments(command), executeCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session, executorService);

        return session;
    }

    /**
     * <p>Asynchronously executes FFmpeg command provided. Space character is used to split command
     * into arguments. You can use single or double quote characters to specify arguments inside
     * your command.
     *
     * @param command            FFmpeg command
     * @param executeCallback    callback that will be called when the execution is completed
     * @param logCallback        callback that will receive logs
     * @param statisticsCallback callback that will receive statistics
     * @param executorService    executor service that will be used to run this asynchronous operation
     * @return FFmpeg session created for this execution
     */
    public static FFmpegSession executeAsync(final String command,
                                             final ExecuteCallback executeCallback,
                                             final LogCallback logCallback,
                                             final StatisticsCallback statisticsCallback,
                                             final ExecutorService executorService) {
        final FFmpegSession session = new FFmpegSession(parseArguments(command), executeCallback, logCallback, statisticsCallback);

        FFmpegKitConfig.asyncFFmpegExecute(session, executorService);

        return session;
    }

    /**
     * <p>Cancels all running sessions.
     *
     * <p>This function does not wait for termination to complete and returns immediately.
     */
    public static void cancel() {

        /*
         * ZERO (0) IS A SPECIAL SESSION ID
         * WHEN IT IS PASSED TO THIS METHOD, A SIGINT IS GENERATED WHICH CANCELS ALL ONGOING
         * SESSIONS
         */
        FFmpegKitConfig.nativeFFmpegCancel(0);
    }

    /**
     * <p>Cancels the session specified with <code>sessionId</code>.
     *
     * <p>This function does not wait for termination to complete and returns immediately.
     *
     * @param sessionId id of the session that will be cancelled
     */
    public static void cancel(final long sessionId) {
        FFmpegKitConfig.nativeFFmpegCancel(sessionId);
    }

    /**
     * <p>Lists all FFmpeg sessions in the session history.
     *
     * @return all FFmpeg sessions in the session history
     */
    public static List<FFmpegSession> listSessions() {
        return FFmpegKitConfig.getFFmpegSessions();
    }

    /**
     * <p>Parses the given command into arguments. Uses space character to split the arguments.
     * Supports single and double quote characters.
     *
     * @param command string command
     * @return array of arguments
     */
    public static String[] parseArguments(final String command) {
        final List<String> argumentList = new ArrayList<>();
        StringBuilder currentArgument = new StringBuilder();

        boolean singleQuoteStarted = false;
        boolean doubleQuoteStarted = false;

        for (int i = 0; i < command.length(); i++) {
            final Character previousChar;
            if (i > 0) {
                previousChar = command.charAt(i - 1);
            } else {
                previousChar = null;
            }
            final char currentChar = command.charAt(i);

            if (currentChar == ' ') {
                if (singleQuoteStarted || doubleQuoteStarted) {
                    currentArgument.append(currentChar);
                } else if (currentArgument.length() > 0) {
                    argumentList.add(currentArgument.toString());
                    currentArgument = new StringBuilder();
                }
            } else if (currentChar == '\'' && (previousChar == null || previousChar != '\\')) {
                if (singleQuoteStarted) {
                    singleQuoteStarted = false;
                } else if (doubleQuoteStarted) {
                    currentArgument.append(currentChar);
                } else {
                    singleQuoteStarted = true;
                }
            } else if (currentChar == '\"' && (previousChar == null || previousChar != '\\')) {
                if (doubleQuoteStarted) {
                    doubleQuoteStarted = false;
                } else if (singleQuoteStarted) {
                    currentArgument.append(currentChar);
                } else {
                    doubleQuoteStarted = true;
                }
            } else {
                currentArgument.append(currentChar);
            }
        }

        if (currentArgument.length() > 0) {
            argumentList.add(currentArgument.toString());
        }

        return argumentList.toArray(new String[0]);
    }

    /**
     * <p>Concatenates arguments into a string adding a space character between two arguments.
     *
     * @param arguments arguments
     * @return concatenated string containing all arguments
     */
    public static String argumentsToString(final String[] arguments) {
        if (arguments == null) {
            return "null";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(arguments[i]);
        }

        return stringBuilder.toString();
    }

}
