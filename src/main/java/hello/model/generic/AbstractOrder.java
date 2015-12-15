package hello.model.generic;

import java.util.Date;
import java.util.List;



public abstract class AbstractOrder<T extends AbstractItem>
extends               AbstractContent {

    private final Date                  orderDate;
    private final List<LazyRelation<T>> items;


    public AbstractOrder(Long id, Date orderDate, List<LazyRelation<T>> items) {
        super(id);
        this.orderDate = orderDate;
        this.items = items;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public List<LazyRelation<T>> getItems() {
        return items;
    }
    
    @Override
    public String toString() {
        return String.format(
                "%s[id=%d, items='%s']", 
                this.getClass().getSimpleName(), id, items);
    }
}