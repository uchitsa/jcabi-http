/*
 * Copyright (c) 2011-2025, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.http.wire;

import com.jcabi.http.Request;
import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ETagCachingWire}.
 * @since 2.0
 */
final class ETagCachingWireTest {

    /**
     * ETagCachingWire can take content from cache.
     * @throws IOException If something goes wrong inside
     */
    @Test
    void takesContentFromCache() throws IOException {
        final String body = "sample content";
        final MkContainer container = new MkGrizzlyContainer()
            .next(
                new MkAnswer.Simple(body)
                .withHeader(HttpHeaders.ETAG, "3e25")
            )
            .next(
                new MkAnswer.Simple("")
                .withStatus(HttpURLConnection.HTTP_NOT_MODIFIED)
            )
            .start();
        final Request req =
            new JdkRequest(container.home()).through(ETagCachingWire.class);
        req
            .fetch()
            .as(RestResponse.class)
            .assertStatus(HttpURLConnection.HTTP_OK)
            .assertBody(Matchers.equalTo(body));
        req
            .fetch()
            .as(RestResponse.class)
            .assertStatus(HttpURLConnection.HTTP_OK)
            .assertBody(Matchers.equalTo(body));
        container.stop();
    }

    /**
     * ETagCachingWire can detect content modification.
     * @throws IOException If something goes wrong inside
     */
    @Test
    void detectsContentModification() throws IOException {
        final String before = "before change";
        final String after = "after change";
        final MkContainer container = new MkGrizzlyContainer()
            .next(
                new MkAnswer.Simple(before)
                    .withHeader(HttpHeaders.ETAG, "3e26")
            )
            .next(
                new MkAnswer.Simple(after)
                    .withHeader(HttpHeaders.ETAG, "3e27")
            )
            .start();
        final Request req =
            new JdkRequest(container.home())
                .through(ETagCachingWire.class);
        req
            .fetch()
            .as(RestResponse.class)
            .assertStatus(HttpURLConnection.HTTP_OK)
            .assertBody(Matchers.equalTo(before));
        req
            .fetch()
            .as(RestResponse.class)
            .assertStatus(HttpURLConnection.HTTP_OK)
            .assertBody(Matchers.equalTo(after));
        container.stop();
    }
}
