/*
 * Copyright (c) 2020-2021 Taner Sener
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

import java.util.LinkedList;
import java.util.List;

/**
 * <p>An FFmpeg session.
 */
public class FFmpegSession extends AbstractSession implements Session {

    /**
     * Session specific statistics callback function.
     */
    private final StatisticsCallback statisticsCallback;

    /**
     * Statistics entries received for this session.
     */
    private final List<Statistics> statistics;

    /**
     * Statistics entry lock.
     */
    private final Object statisticsLock;

    /**
     * Builds a new FFmpeg session.
     *
     * @param arguments command arguments
     */
    public FFmpegSession(final String[] arguments) {
        this(arguments, null);
    }

    /**
     * Builds a new FFmpeg session.
     *
     * @param arguments       command arguments
     * @param executeCallback session specific execute callback function
     */
    public FFmpegSession(final String[] arguments, final ExecuteCallback executeCallback) {
        this(arguments, executeCallback, null, null);
    }

    /**
     * Builds a new FFmpeg session.
     *
     * @param arguments          command arguments
     * @param executeCallback    session specific execute callback function
     * @param logCallback        session specific log callback function
     * @param statisticsCallback session specific statistics callback function
     */
    public FFmpegSession(final String[] arguments,
                         final ExecuteCallback executeCallback,
                         final LogCallback logCallback,
                         final StatisticsCallback statisticsCallback) {
        this(arguments, executeCallback, logCallback, statisticsCallback, FFmpegKitConfig.getLogRedirectionStrategy());
    }

    /**
     * Builds a new FFmpeg session.
     *
     * @param arguments              command arguments
     * @param executeCallback        session specific execute callback function
     * @param logCallback            session specific log callback function
     * @param statisticsCallback     session specific statistics callback function
     * @param logRedirectionStrategy session specific log redirection strategy
     */
    public FFmpegSession(final String[] arguments,
                         final ExecuteCallback executeCallback,
                         final LogCallback logCallback,
                         final StatisticsCallback statisticsCallback,
                         final LogRedirectionStrategy logRedirectionStrategy) {
        super(arguments, executeCallback, logCallback, logRedirectionStrategy);

        this.statisticsCallback = statisticsCallback;

        this.statistics = new LinkedList<>();
        this.statisticsLock = new Object();
    }

    /**
     * Returns the session specific statistics callback function.
     *
     * @return session specific statistics callback function
     */
    public StatisticsCallback getStatisticsCallback() {
        return statisticsCallback;
    }

    /**
     * Returns all statistics entries generated for this session. If there are asynchronous
     * messages that are not delivered yet, this method waits for them until the given timeout.
     *
     * @param waitTimeout wait timeout for asynchronous messages in milliseconds
     * @return list of statistics entries generated for this session
     */
    public List<Statistics> getAllStatistics(final int waitTimeout) {
        waitForAsynchronousMessagesInTransmit(waitTimeout);

        if (thereAreAsynchronousMessagesInTransmit()) {
            android.util.Log.i(FFmpegKitConfig.TAG, String.format("getAllStatistics was called to return all statistics but there are still statistics being transmitted for session id %d.", sessionId));
        }

        return getStatistics();
    }

    /**
     * Returns all statistics entries generated for this session. If there are asynchronous
     * messages that are not delivered yet, this method waits for them until
     * {@link #DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT} expires.
     *
     * @return list of statistics entries generated for this session
     */
    public List<Statistics> getAllStatistics() {
        return getAllStatistics(DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT);
    }

    /**
     * Returns all statistics entries delivered for this session. Note that if there are
     * asynchronous messages that are not delivered yet, this method will not wait for
     * them and will return immediately.
     *
     * @return list of statistics entries received for this session
     */
    public List<Statistics> getStatistics() {
        synchronized (statisticsLock) {
            return statistics;
        }
    }

    /**
     * Returns the last received statistics entry.
     *
     * @return the last received statistics entry or null if there are not any statistics entries
     * received
     */
    public Statistics getLastReceivedStatistics() {
        synchronized (statisticsLock) {
            if (statistics.size() > 0) {
                return statistics.get(0);
            } else {
                return null;
            }
        }
    }

    /**
     * Adds a new statistics entry for this session.
     *
     * @param statistics statistics entry
     */
    public void addStatistics(final Statistics statistics) {
        synchronized (statisticsLock) {
            this.statistics.add(statistics);
        }
    }

    @Override
    public boolean isFFmpeg() {
        return true;
    }

    @Override
    public boolean isFFprobe() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("FFmpegSession{");
        stringBuilder.append("sessionId=");
        stringBuilder.append(sessionId);
        stringBuilder.append(", createTime=");
        stringBuilder.append(createTime);
        stringBuilder.append(", startTime=");
        stringBuilder.append(startTime);
        stringBuilder.append(", endTime=");
        stringBuilder.append(endTime);
        stringBuilder.append(", arguments=");
        stringBuilder.append(FFmpegKit.argumentsToString(arguments));
        stringBuilder.append(", logs=");
        stringBuilder.append(getLogsAsString());
        stringBuilder.append(", state=");
        stringBuilder.append(state);
        stringBuilder.append(", returnCode=");
        stringBuilder.append(returnCode);
        stringBuilder.append(", failStackTrace=");
        stringBuilder.append('\'');
        stringBuilder.append(failStackTrace);
        stringBuilder.append('\'');
        stringBuilder.append('}');

        return stringBuilder.toString();
    }

}
