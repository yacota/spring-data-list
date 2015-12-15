package hello.model;

import hello.model.generic.AbstractOrder;
import hello.model.generic.LazyRelation;
import java.util.Date;
import java.util.List;

public class Order extends AbstractOrder<Item> {
    
    public Order(Long id, Date orderDate, List<LazyRelation<Item>> items) {
        super(id, orderDate, items);
    }

}