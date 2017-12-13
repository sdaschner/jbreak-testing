package com.sebastian_daschner.coffee_shop;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.rules.ExternalResource;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;

public class Processor extends ExternalResource {

    private final Map<URI, Set<String>> requestedStatuses = new HashMap<>();

    @Override
    protected void before() {
        configureFor("192.168.99.100", 31425);
        resetAllRequests();

        stubFor(post("/coffee-processor/resources/processes")
                .willReturn(jsonResponse("PREPARING")));
    }

    public void answerForId(URI uri, String answer) {
        String id = extractId(uri);
        stubFor(post("/coffee-processor/resources/processes")
                .withRequestBody(jsonRequest(id))
                .willReturn(jsonResponse(answer)));
    }

    private String extractId(URI uri) {
        String string = uri.toString();
        return string.substring(string.lastIndexOf('/') + 1);
    }

    private StringValuePattern jsonRequest(String id) {
        return equalToJson("{\"order\":\"" + id + "\"}", true, true);
    }

    private ResponseDefinitionBuilder jsonResponse(String answer) {
        return okForJson(singletonMap("status", answer));
    }

    public void waitForInvocation(URI orderUri, String requestedStatus) {
        addRequestedStatus(orderUri, requestedStatus);
        final long timeout = System.currentTimeMillis() + 60_000;

        while (retrieveRequestedStatuses(orderUri, requestedStatus).isEmpty()) {
            LockSupport.parkNanos(2_000_000_000L);
            if (System.currentTimeMillis() > timeout)
                throw new AssertionError("Processing for order " + orderUri + " wasn't invoked within timeout!");
        }
    }

    private void addRequestedStatus(URI orderUri, String requestedStatus) {
        requestedStatuses.computeIfAbsent(orderUri, c -> new HashSet<>())
                .add(requestedStatus);
    }

    private List<String> retrieveRequestedStatuses(URI orderUri, String requestedStatus) {
        String id = extractId(orderUri);
        return getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.getUrl().equals("/coffee-processor/resources/processes"))
                .sorted(comparing(LoggedRequest::getLoggedDate))
                .map(LoggedRequest::getBodyAsString)
                .map(this::readJson)
                .filter(o -> contains(o, id, requestedStatus))
                .map(this::extractOrder)
                .collect(Collectors.toList());
    }

    private JsonObject readJson(String s) {
        return Json.createReader(new StringReader(s)).readObject();
    }

    private boolean contains(JsonObject object, String id, String requestedStatus) {
        return extractOrder(object).equals(id) && extractStatus(object).equals(requestedStatus);
    }

    private String extractOrder(JsonObject object) {
        return object.getString("order");
    }

    private String extractStatus(JsonObject object) {
        return object.getString("status");
    }

    @Override
    protected void after() {
        resetAllRequests();
    }

}
