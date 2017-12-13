package com.sebastian_daschner.coffee_shop.entity;

public class Order {

    private String id;
    private String type;
    private String origin;
    private String status;

    private Order() {
        // for jackson
    }

    public Order(String type, String origin) {
        this.type = type;
        this.origin = origin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
