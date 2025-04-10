/*
 * SPDX-FileCopyrightText: Copyright (c) 2011-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package com.jcabi.http.mock;

import com.jcabi.http.Request;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import com.jcabi.http.wire.VerboseWire;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.util.NoSuchElementException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsAnything;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Test case for {@link MkContainer}.
 * @since 1.0
 */
final class MkContainerTest {

    /**
     * MkContainer can return required answers.
     * @throws Exception If something goes wrong inside
     */
    @Test
    void worksAsServletContainer() throws Exception {
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(
                new MkAnswer.Simple(HttpURLConnection.HTTP_OK, "works fine!")
            ).start();
            new JdkRequest(container.home())
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .assertBody(Matchers.startsWith("works"));
            final MkQuery query = container.take();
            MatcherAssert.assertThat(
                "should be GET method",
                query.method(),
                Matchers.equalTo(Request.GET)
            );
        }
    }

    /**
     * MkContainer can understand duplicate headers.
     * @throws Exception If something goes wrong inside
     */
    @Test
    void understandsDuplicateHeaders() throws Exception {
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(new MkAnswer.Simple("")).start();
            final String header = "X-Something";
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .header(header, MediaType.TEXT_HTML)
                .header(header, MediaType.TEXT_XML)
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK);
            final MkQuery query = container.take();
            MatcherAssert.assertThat(
                "should has size 2",
                query.headers().get(header),
                Matchers.hasSize(2)
            );
        }
    }

    /**
     * MkContainer can return certain answers for matching conditions.
     * @throws Exception If something goes wrong inside.
     */
    @Test
    void answersConditionally() throws Exception {
        final String match = "matching";
        final String mismatch = "not matching";
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(
                new MkAnswer.Simple(mismatch),
                Matchers.not(new IsAnything<MkQuery>())
            ).next(new MkAnswer.Simple(match), new IsAnything<MkQuery>())
                .start();
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .assertBody(
                    Matchers.allOf(
                        Matchers.is(match),
                        Matchers.not(mismatch)
                    )
                );
        }
    }

    /**
     * MkContainer can return a correct binary answers.
     * @throws Exception If something goes wrong inside.
     */
    @Test
    void answersBinary() throws Exception {
        final byte[] body = {0x00, 0x01, 0x45, 0x21, (byte) 0xFF};
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(
                new MkAnswer.Simple(HttpURLConnection.HTTP_OK)
                    .withBody(body)
            ).start();
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .assertBinary(Matchers.is(body));
        }
    }

    /**
     * MkContainer returns HTTP 500 if no answers match.
     */
    @Test
    void returnsErrorIfNoMatches() {
        Assertions.assertThrows(
            NoSuchElementException.class,
            new Executable() {
                @Override
                public void execute() throws Throwable {
                    try (MkContainer container = new MkGrizzlyContainer()) {
                        container.next(
                            new MkAnswer.Simple("not supposed to match"),
                            Matchers.not(new IsAnything<MkQuery>())
                        ).start();
                        new JdkRequest(container.home())
                            .through(VerboseWire.class)
                            .fetch()
                            .as(RestResponse.class)
                            .assertStatus(
                                HttpURLConnection.HTTP_INTERNAL_ERROR
                            );
                        container.take();
                    }
                }
            }
        );
    }

    /**
     * MkContainer can answer multiple times for matching requests.
     * @throws Exception If something goes wrong inside
     */
    @Test
    void canAnswerMultipleTimes() throws Exception {
        final String body = "multiple";
        final int times = 5;
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(
                new MkAnswer.Simple(body),
                new IsAnything<MkQuery>(),
                times
            ).start();
            final Request req = new JdkRequest(container.home())
                .through(VerboseWire.class);
            for (int idx = 0; idx < times; idx += 1) {
                req.fetch().as(RestResponse.class)
                    .assertStatus(HttpURLConnection.HTTP_OK)
                    .assertBody(Matchers.is(body));
            }
        }
    }

    /**
     * MkContainer can prioritize multiple matching answers by using the
     * first matching request.
     * @throws Exception If something goes wrong inside.
     */
    @Test
    void prioritizesMatchingAnswers() throws Exception {
        final String first = "first";
        final String second = "second";
        try (MkContainer container = new MkGrizzlyContainer()) {
            container
                .next(new MkAnswer.Simple(first), new IsAnything<MkQuery>())
                .next(new MkAnswer.Simple(second), new IsAnything<MkQuery>())
                .start();
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .assertBody(
                    Matchers.allOf(
                        Matchers.is(first),
                        Matchers.not(second)
                    )
                );
        }
    }

    /**
     * MkContainer can return the query that matched a certain response.
     * @throws Exception If something goes wrong inside
     */
    @Test
    void takesMatchingQuery() throws Exception {
        final String request = "reqBodyMatches";
        final String response = "respBodyMatches";
        try (MkContainer container = new MkGrizzlyContainer()) {
            container
                .next(new MkAnswer.Simple(response))
                .next(new MkAnswer.Simple("bleh"))
                .start();
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .method(HttpMethod.POST)
                .body().set(request).back()
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK);
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .method(HttpMethod.POST)
                .body().set("reqBodyMismatches").back()
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK);
            MatcherAssert.assertThat(
                "should match the answer body",
                container.take(MkAnswerMatchers.hasBody(Matchers.is(response))),
                MkQueryMatchers.hasBody(Matchers.is(request))
            );
        }
    }

    /**
     * MkContainer can return all queries that matched a certain response.
     * @throws Exception If something goes wrong inside
     */
    @Test
    @SuppressWarnings("unchecked")
    void takesAllMatchingQueries() throws Exception {
        final String match = "multipleRequestMatches";
        final String mismatch = "multipleRequestNotMatching";
        final String response = "multipleResponseMatches";
        try (MkContainer container = new MkGrizzlyContainer()) {
            container.next(
                new MkAnswer.Simple(response),
                MkQueryMatchers.hasBody(Matchers.is(match)),
                2
            ).next(new MkAnswer.Simple("blaa")).start();
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .method(HttpMethod.POST)
                .body().set(match).back()
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .back()
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK);
            new JdkRequest(container.home())
                .through(VerboseWire.class)
                .method(HttpMethod.POST)
                .body().set(mismatch).back()
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK);
            MatcherAssert.assertThat(
                "should match all bodies",
                container.takeAll(
                    MkAnswerMatchers.hasBody(Matchers.is(response))
                ),
                Matchers.allOf(
                    Matchers.<MkQuery>iterableWithSize(2),
                    Matchers.hasItems(
                        MkQueryMatchers.hasBody(Matchers.is(match))
                    ),
                    Matchers.not(
                        Matchers.hasItems(
                            MkQueryMatchers.hasBody(Matchers.is(mismatch))
                        )
                    )
                )
            );
        }
    }
}
