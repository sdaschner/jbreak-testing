package com.sebastian_daschner.coffee_shop;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonMap;

public class ProcessorSystem {

    public ProcessorSystem() {
        configureFor("coffee-processor.test.kubernetes.local", 80);
        reset();

        stubFor(post("/coffee-processor/resources/processes").willReturn(responseJson("PREPARING")));
    }

    private ResponseDefinitionBuilder responseJson(String status) {
        return okForJson(singletonMap("status", status));
    }

    public void answerForId(URI uri, String answer) {
        String orderId = extractId(uri);
        stubFor(post("/coffee-processor/resources/processes")
                .withRequestBody(requestJson(orderId))
                .willReturn(responseJson(answer)));
    }

    private String extractId(URI uri) {
        String string = uri.toString();
        return string.substring(string.lastIndexOf('/') + 1);
    }

    private StringValuePattern requestJson(String id) {
        return equalToJson("{\"order\":\"" + id + "\"}", true, true);
    }

    private StringValuePattern requestJson(String id, String status) {
        return equalToJson("{\"order\":\"" + id + "\",\"status\":\"" + status + "\"}", true, true);
    }

    public void waitForInvocation(URI orderUri, String requestedStatus) {
        long timeout = System.currentTimeMillis() + 60_000L;
        while (!wasStatusRequested(orderUri, requestedStatus)) {
            LockSupport.parkNanos(2_000_000_000L);
            if (System.currentTimeMillis() > timeout)
                throw new AssertionError("Processing for order " + orderUri + " wasn't invoked within timeout");
        }
    }

    private boolean wasStatusRequested(URI orderUri, String status) {
        String orderId = extractId(orderUri);
        List<LoggedRequest> requests = findAll(postRequestedFor(urlEqualTo("/coffee-processor/resources/processes"))
                .withRequestBody(requestJson(orderId, status)));
        return !requests.isEmpty();
    }

    public void reset() {
        resetAllRequests();
    }

}
