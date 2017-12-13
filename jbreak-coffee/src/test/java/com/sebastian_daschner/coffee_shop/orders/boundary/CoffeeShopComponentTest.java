package com.sebastian_daschner.coffee_shop.orders.boundary;

import com.sebastian_daschner.coffee_shop.orders.control.OrderProcessorComponent;
import com.sebastian_daschner.coffee_shop.orders.entity.CoffeeType;
import com.sebastian_daschner.coffee_shop.orders.entity.Order;
import com.sebastian_daschner.coffee_shop.orders.entity.Origin;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CoffeeShopComponentTest {

    private CoffeeShopComponent coffeeShop;
    private OrderProcessorComponent orderProcessor;

    @Before
    public void setUp() {
        orderProcessor = new OrderProcessorComponent();
        coffeeShop = new CoffeeShopComponent(orderProcessor);
    }

    @Test
    public void testCreateOrder() {
        Order order = new Order();
        coffeeShop.createOrder(order);
        coffeeShop.verifyCreateOrder(order);
    }

    @Test
    public void testProcessUnfinishedOrders() {
        List<Order> orders = Arrays.asList(new Order(UUID.randomUUID(), CoffeeType.ESPRESSO, new Origin("Colombia")),
                new Order(UUID.randomUUID(), CoffeeType.ESPRESSO, new Origin("Ethiopia")));
        coffeeShop.answerForUnfinishedOrders(orders);

        coffeeShop.processUnfinishedOrders();

        coffeeShop.verifyProcessUnfinishedOrders();
        orderProcessor.verifyProcessOrders(orders);
    }

}
