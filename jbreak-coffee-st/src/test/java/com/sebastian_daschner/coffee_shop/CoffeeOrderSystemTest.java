package com.sebastian_daschner.coffee_shop;

import com.sebastian_daschner.coffee_shop.entity.Order;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CoffeeOrderSystemTest {

    @Rule
    public CoffeeShop coffeeShop = new CoffeeShop();

    @Rule
    public Processor processor = new Processor();

    @Test
    public void createVerifyOrder() {
        List<URI> originalOrders = coffeeShop.getOrders();

        Order order = new Order("Espresso", "Colombia");
        URI orderUri = coffeeShop.createOrder(order);

        Order loadedOrder = coffeeShop.getOrder(orderUri);
        assertThat(loadedOrder).isEqualToComparingOnlyGivenFields(order, "type", "origin");

        assertThat(coffeeShop.getOrders()).hasSize(originalOrders.size() + 1);
    }

    @Test
    public void createOrderCheckStatusUpdate() {
        Order order = new Order("Espresso", "Colombia");
        URI orderUri = coffeeShop.createOrder(order);

        processor.answerForId(orderUri, "PREPARING");

        Order loadedOrder = coffeeShop.getOrder(orderUri);
        assertThat(loadedOrder).isEqualToComparingOnlyGivenFields(order, "type", "origin");

        loadedOrder = waitForProcessAndGet(orderUri, "PREPARING");
        assertThat(loadedOrder.getStatus()).isEqualTo("Preparing");

        processor.answerForId(orderUri, "FINISHED");

        loadedOrder = waitForProcessAndGet(orderUri, "FINISHED");
        assertThat(loadedOrder.getStatus()).isEqualTo("Finished");
    }

    private Order waitForProcessAndGet(URI orderUri, String requestedStatus) {
        processor.waitForInvocation(orderUri, requestedStatus);
        return coffeeShop.getOrder(orderUri);
    }

}