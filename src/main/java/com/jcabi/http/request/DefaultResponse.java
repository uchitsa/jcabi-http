/*
 * SPDX-FileCopyrightText: Copyright (c) 2011-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package com.jcabi.http.request;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.http.Request;
import com.jcabi.http.RequestBody;
import com.jcabi.http.Response;
import com.jcabi.immutable.Array;
import com.jcabi.log.Logger;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.EqualsAndHashCode;

/**
 * Default implementation of {@link com.jcabi.http.Response}.
 *
 * @since 1.0
 */
@Immutable
@EqualsAndHashCode(of = { "req", "code", "phrase", "hdrs", "content" })
@Loggable(Loggable.DEBUG)
public final class DefaultResponse implements Response {

    /**
     * UTF-8 error marker.
     */
    private static final String ERR = "\uFFFD";

    /**
     * Request.
     */
    private final transient Request req;

    /**
     * Status code.
     */
    private final transient int code;

    /**
     * Reason phrase.
     */
    private final transient String phrase;

    /**
     * Headers.
     */
    private final transient Array<Map.Entry<String, String>> hdrs;

    /**
     * Content received.
     */
    @Immutable.Array
    //@checkstyle ParameterNumber (15 lines)
    private final transient byte[] content;

    /**
     * Public ctor.
     * @param request The request
     * @param status HTTP status
     * @param reason HTTP reason phrase
     * @param headers HTTP headers
     * @param body Body of HTTP response
     */
    public DefaultResponse(final Request request, final int status,
        final String reason, final Array<Map.Entry<String, String>> headers,
        final byte[] body) {
        this.req = request;
        this.code = status;
        this.phrase = reason;
        this.hdrs = headers;
        this.content = body.clone();
    }

    @Override
    public Request back() {
        return this.req;
    }

    @Override
    public int status() {
        return this.code;
    }

    @Override
    public String reason() {
        return this.phrase;
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, List<String>> headers() {
        final ConcurrentMap<String, List<String>> map =
            new ConcurrentHashMap<>(0);
        for (final Map.Entry<String, String> header : this.hdrs) {
            map.putIfAbsent(header.getKey(), new LinkedList<String>());
            map.get(header.getKey()).add(header.getValue());
        }
        return map;
    }

    @Override
    public String body() {
        final String body = new String(this.content, StandardCharsets.UTF_8);
        if (body.contains(DefaultResponse.ERR)) {
            throw new IllegalStateException(
                Logger.format(
                    "broken Unicode text at line #%d in '%[text]s' (%d bytes)",
                    body.length() - body.replace("\n", "").length(),
                    body,
                    this.content.length
                )
            );
        }
        return body;
    }

    @Override
    public byte[] binary() {
        return this.content.clone();
    }

    // @checkstyle MethodName (4 lines)
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public <T extends Response> T as(final Class<T> type) {
        try {
            return type.getDeclaredConstructor(Response.class)
                .newInstance(this);
        } catch (final InstantiationException
            | IllegalAccessException | NoSuchMethodException
            | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
    public String toString() {
        final StringBuilder text = new StringBuilder(0)
            .append(this.code).append(' ')
            .append(this.phrase)
            .append(" [")
            .append(this.back().uri().get())
            .append("]\n");
        for (final Map.Entry<String, String> header : this.hdrs) {
            text.append(
                Logger.format(
                    "%s: %s\n",
                    header.getKey(),
                    header.getValue()
                )
            );
        }
        return text.append('\n')
            .append(new RequestBody.Printable(this.content))
            .toString();
    }

}
