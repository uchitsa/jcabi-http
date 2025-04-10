/*
 * SPDX-FileCopyrightText: Copyright (c) 2011-2025 Yegor Bugayenko
 * SPDX-License-Identifier: MIT
 */
package com.jcabi.http.response;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcabi.http.request.FakeRequest;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Test case for {@link JacksonResponse}.
 *
 * @since 1.17
 */
final class JacksonResponseTest {
    /**
     * JacksonResponse can read and return a JSON document.
     *
     * @throws IOException If anything goes wrong when parsing.
     */
    @Test
    void canReadJsonDocument() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\n\t\r\"foo-foo\":2,\n\"bar\":\"\u20ac\"}")
            .fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should be 2",
            response.json().read().path("foo-foo").asInt(),
            Matchers.equalTo(2)
        );
        MatcherAssert.assertThat(
            "should be '\u20ac'",
            response.json().read().path("bar").asText(),
            Matchers.equalTo("\u20ac")
        );
    }

    /**
     * JacksonResponse can read control characters.
     *
     * @throws IOException If anything goes wrong when parsing.
     */
    @Test
    void canParseUnquotedControlCharacters() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\"test\":\n\"\u001Fblah\uFFFDcwhoa\u0000!\"}")
            .fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should be '\u001Fblah\uFFFDcwhoa\u0000!'",
            response.json().readObject().get("test").asText(),
            Matchers.is("\u001Fblah\uFFFDcwhoa\u0000!")
        );
    }

    /**
     * If there's a problem parsing the body as JSON the error handling is done
     * by Jackson.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void invalidJsonErrorHandlingIsLeftToJackson() throws IOException {
        final String body = "{test:[]}";
        final String err = "was expecting double-quote to start field name";
        final JacksonResponse response = new FakeRequest()
            .withBody(body).fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should contains error 'was expecting double-quote to start field name'",
            Assertions.assertThrows(
                IOException.class,
                new Executable() {
                    @Override
                    public void execute() throws Throwable {
                        response.json().read();
                    }
                }
            ),
            Matchers.hasProperty(
                "message",
                Matchers.containsString(err)
            )
        );
    }

    /**
     * If there's a problem parsing the body as JSON the error handling is done
     * by Jackson.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void invalidJsonArrayErrorHandlingIsLeftToJackson()
        throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\"anInvalidArrayTest\":[}")
            .fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should contains error 'Unexpected close marker'",
            Assertions.assertThrows(
                IOException.class,
                new Executable() {
                    @Override
                    public void execute() throws Throwable {
                        response.json().readArray();
                    }
                }
            ),
            Matchers.hasToString(
                Matchers.containsString("Unexpected close marker")
            )
        );
    }

    /**
     * If the parsed JSON is a valid one but an array an exception is raised.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void cannotReadJsonAsArrayIfNotOne() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\"objectIsNotArray\": \"It's not!\"}")
            .fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should contains 'Cannot read as an array. The JSON is not a valid array.'",
            Assertions.assertThrows(
                IOException.class,
                new Executable() {
                    @Override
                    public void execute() throws Throwable {
                        response.json().readArray();
                    }
                }
            ),
            Matchers.<IOException>hasToString(
                Matchers.containsString(
                    "Cannot read as an array. The JSON is not a valid array."
                )
            )
        );
    }

    /**
     * Can retrieve the JSON as an array node if it's a valid one.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void canReadAsArrayIfOne() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("[\"one\", \"two\"]")
            .fetch().as(JacksonResponse.class);
        final ArrayNode array = response.json().readArray();
        MatcherAssert.assertThat(
            "should be 'one'", array.get(0).asText(), Matchers.is("one")
        );
        MatcherAssert.assertThat(
            "should be 'one'", array.get(1).asText(), Matchers.is("two")
        );
    }

    /**
     * If there's a problem parsing the body as JSON the error handling is done
     * by Jackson.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void invalidJsonObjectErrorIsLeftToJackson() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\"anInvalidObjectTest\":{}")
            .fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should contains error 'Unexpected end-of-input: expected close marker for Object",
            Assertions.assertThrows(
                IOException.class,
                new Executable() {
                    @Override
                    public void execute() throws Throwable {
                        response.json().readObject();
                    }
                }
            ),
            Matchers.<IOException>hasToString(
                Matchers.containsString(
                    "Unexpected end-of-input: expected close marker for Object"
                )
            )
        );
    }

    /**
     * If the parsed JSON is a valid one but an object an exception is raised.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void cannotReadJsonAsObjectIfNotOne() throws IOException {
        final String body = "[\"arrayIsNotObject\", \"It's not!\"]";
        final JacksonResponse response = new FakeRequest()
            .withBody(body).fetch().as(JacksonResponse.class);
        MatcherAssert.assertThat(
            "should contains error 'Cannot read as an object. The JSON is not a valid object.",
            Assertions.assertThrows(
                IOException.class,
                new Executable() {
                    @Override
                    public void execute() throws Throwable {
                        response.json().readObject();
                    }
                }
            ),
            Matchers.<IOException>hasToString(
                Matchers.containsString(
                    "Cannot read as an object. The JSON is not a valid object."
                )
            )
        );
    }

    /**
     * Can retrieve the JSON as an object node if it's a valid one.
     *
     * @throws IOException If anything goes wrong.
     */
    @Test
    void canReadAsObjectIfOne() throws IOException {
        final JacksonResponse response = new FakeRequest()
            .withBody("{\"hooray\": \"Got milk?\"}")
            .fetch().as(JacksonResponse.class);
        final ObjectNode object = response.json().readObject();
        MatcherAssert.assertThat(
            "should contains 'Got milk?", object.get("hooray").asText(), Matchers.is("Got milk?")
        );
    }
}
