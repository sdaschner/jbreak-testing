package com.sebastian_daschner.coffee_shop;

import com.sebastian_daschner.coffee_shop.entity.Order;
import org.junit.rules.ExternalResource;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CoffeeShopSystem extends ExternalResource {

    private static final int STARTUP_TIMEOUT = 30;
    private static final int STARTUP_PING_DELAY = 2;

    private String ordersUri = "http://192.168.99.100:30585/jbreak-coffee/resources/orders";
    private Pattern orderUriPattern = Pattern.compile(ordersUri + "/[a-z0-9\\-]+");
    private WebTarget ordersTarget;
    private Client client;

    public URI createOrder(Order order) {
        final Response response = postOrder(order);
        assertStatus(response, Response.Status.CREATED);

        final URI location = response.getLocation();
        assertOrderUri(location);

        return location;
    }

    private Response postOrder(Order order) {
        return ordersTarget.request().post(Entity.json(order));
    }

    public Order getOrder(final URI orderUri) {
        final Response response = getOrderResponse(orderUri);
        assertStatus(response, Response.Status.OK);
        return response.readEntity(Order.class);
    }

    private Response getOrderResponse(final URI orderUri) {
        return client.target(orderUri).request(MediaType.APPLICATION_JSON).get();
    }

    public List<URI> getOrders() {
        final Response response = getOrdersResponse();
        assertStatus(response, Response.Status.OK);
        return extractOrderUris(response);
    }

    private Response getOrdersResponse() {
        return ordersTarget.request(MediaType.APPLICATION_JSON_TYPE).get();
    }

    private List<URI> extractOrderUris(final Response response) {
        final JsonArray orders = response.readEntity(JsonArray.class);
        return orders.getValuesAs(JsonObject.class).stream()
                .map(o -> o.getString("_self"))
                .map(URI::create).collect(Collectors.toList());
    }

    private void assertStatus(final Response response, final Response.Status expectedStatus) {
        assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());
    }

    private void assertOrderUri(final URI location) {
        assertThat(location).isNotNull();
        assertThat(orderUriPattern.matcher(location.toString()).matches())
                .as("URI %s doesn't match pattern: %s", location, orderUriPattern.pattern())
                .isTrue();
    }

    @Override
    protected void before() throws Throwable {
        client = ClientBuilder.newClient();
        ordersTarget = client.target(URI.create(ordersUri));
        waitForApplicationStartUp();
    }

    private void waitForApplicationStartUp() {
        final long timeout = System.currentTimeMillis() + STARTUP_TIMEOUT * 1000;
        while (ordersTarget.request().head().getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            System.out.println("waiting for application startup");
            LockSupport.parkNanos(1_000_000_000 * STARTUP_PING_DELAY);
            if (System.currentTimeMillis() > timeout)
                throw new AssertionError("Application wasn't started before timeout!");
        }
    }

}
