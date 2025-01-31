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

import com.jcabi.aspects.Immutable;
import com.jcabi.http.Wire;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.ToString;

/**
 * Wire that caches requests with ETags.
 *
 * <p>This decorator can be used when you want to avoid duplicate
 * requests to load-sensitive resources and server supports ETags, for example:
 *
 * <pre>{@code
 *    String html = new JdkRequest("http://goggle.com")
 *        .through(ETagCachingWire.class)
 *        .fetch()
 *        .body();
 * }</pre>
 *
 * <p>Client will automatically detect if server uses ETags and start adding
 * corresponding If-None-Match to outgoing requests
 *
 * <p>Client will take response from the cache if it is present
 * or will query resource for that.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @since 2.0
 */
@ToString
@Immutable
public final class ETagCachingWire extends AbstractHeaderBasedCachingWire {

    /**
     * Public ctor.
     * @param wire Original wire
     */
    public ETagCachingWire(final Wire wire) {
        super(HttpHeaders.ETAG, HttpHeaders.IF_NONE_MATCH, wire);
    }
}
