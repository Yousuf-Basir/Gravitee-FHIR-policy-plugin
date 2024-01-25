/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class FhirConverterPolicy implements Policy {

    @Override
    public String id() {
        return "fhir-converter";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return ctx.request().onBody(body -> assignFhirBodyContent(ctx, ctx.request().headers(), body, true));
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return ctx.response().onBody(body -> statusOkBodyContent(ctx, ctx.response().headers(), body, false));
    }

    private Maybe<Buffer> assignFhirBodyContent(HttpExecutionContext ctx, HttpHeaders httpHeaders, Maybe<Buffer> body, boolean isRequest) {
        JsonToFhirConverter converter = new JsonToFhirConverter();

        return body
            .flatMap(content -> {
                String bodyString = content.toString();
                String convertedBody = converter.convert(bodyString);
                log.info("Converted body: " + convertedBody);
                return Maybe.just(Buffer.buffer(convertedBody));
            })
            .switchIfEmpty(
                Maybe.fromCallable(() -> {
                    // For method like GET where body is missing, we have to handle the case where the maybe is empty.
                    // It can make sens if in the Flow we have an override method policy that replace the GET by a POST
                    return Buffer.buffer();
                })
            )
            .doOnSuccess(buffer -> httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(buffer.length())))
            .onErrorResumeNext(ioe -> {
                log.debug("Unable to assign body content", ioe);
                return ctx.interruptBodyWith(
                    new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                        .message("Unable to assign body content: " + ioe.getMessage())
                );
            });
    }

    // this method sets body content to { status: "All ok"}
    private Maybe<Buffer> statusOkBodyContent(HttpExecutionContext ctx, HttpHeaders httpHeaders, Maybe<Buffer> body, boolean isRequest) {
        return body
            .flatMap(content -> {
                String convertedBody = "{ \"status\": \"All ok\"}";
                log.info("Converted body: " + convertedBody);
                return Maybe.just(Buffer.buffer(convertedBody));
            })
            .switchIfEmpty(
                Maybe.fromCallable(() -> {
                    // For method like GET where body is missing, we have to handle the case where the maybe is empty.
                    // It can make sens if in the Flow we have an override method policy that replace the GET by a POST
                    return Buffer.buffer();
                })
            )
            .doOnSuccess(buffer -> httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(buffer.length())))
            .onErrorResumeNext(ioe -> {
                log.debug("Unable to assign body content", ioe);
                return ctx.interruptBodyWith(
                    new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                        .message("Unable to assign body content: " + ioe.getMessage())
                );
            });
    }
}
