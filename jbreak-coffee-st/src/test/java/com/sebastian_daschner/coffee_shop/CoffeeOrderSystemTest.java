package com.sebastian_daschner.coffee_shop;

import com.sebastian_daschner.coffee_shop.entity.Order;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CoffeeOrderSystemTest {

    @Rule
    public CoffeeShopSystem coffeeShopSystem = new CoffeeShopSystem();

    @Rule
    public ProcessorSystem processorSystem = new ProcessorSystem();

    @Test
    public void createVerifyOrder() {
        List<URI> originalOrders = coffeeShopSystem.getOrders();

        Order order = new Order("Espresso", "Colombia");
        URI orderUri = coffeeShopSystem.createOrder(order);

        Order loadedOrder = coffeeShopSystem.getOrder(orderUri);
        assertThat(loadedOrder).isEqualToComparingOnlyGivenFields(order, "type", "origin");

        assertThat(coffeeShopSystem.getOrders()).hasSize(originalOrders.size() + 1);
    }

    @Test
    public void createOrderCheckStatusUpdate() {
        Order order = new Order("Espresso", "Colombia");
        URI orderUri = coffeeShopSystem.createOrder(order);

        processorSystem.answerForId(orderUri, "PREPARING");

        Order loadedOrder = coffeeShopSystem.getOrder(orderUri);
        assertThat(loadedOrder).isEqualToComparingOnlyGivenFields(order, "type", "origin");

        loadedOrder = waitForProcessAndGet(orderUri, "PREPARING");
        assertThat(loadedOrder.getStatus()).isEqualTo("Preparing");

        processorSystem.answerForId(orderUri, "FINISHED");

        loadedOrder = waitForProcessAndGet(orderUri, "FINISHED");
        assertThat(loadedOrder.getStatus()).isEqualTo("Finished");
    }

    private Order waitForProcessAndGet(URI orderUri, String requestedStatus) {
        processorSystem.waitForInvocation(orderUri, requestedStatus);
        return coffeeShopSystem.getOrder(orderUri);
    }

}