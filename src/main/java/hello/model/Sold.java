package hello.model;

import hello.model.generic.AbstractContent;

import hello.model.generic.LazyRelation;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class Sold 
extends      AbstractContent {

    private final LazyRelation<Order> lazyOrder;
    @DBRef(lazy = true)
    private final Order               order;

    public Sold(Long id, Order order, LazyRelation<Order> lazyOrder) {
        super(id);
        this.lazyOrder = lazyOrder;
        this.order = order;
    }
    
    public LazyRelation<Order> getLazyOrder() {
        return this.lazyOrder;
    }
     
    public Order getOrder() {
        return this.order;
    }
}