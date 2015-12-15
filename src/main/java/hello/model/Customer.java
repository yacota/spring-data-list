package hello.model;

import java.util.List;

import hello.model.generic.AbstractCustomer;
import hello.model.generic.LazyRelation;

public class Customer extends AbstractCustomer<Order, Item> {


    public Customer(Long id, String firstName, String lastName, List<LazyRelation<Order>> lazyOrders, List<Order> orders) {
        super(id, firstName, lastName, lazyOrders, orders);
    }

}