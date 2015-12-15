package hello.model.generic;

import java.util.List;
import org.springframework.data.mongodb.core.mapping.DBRef;



public abstract class AbstractCustomer<T extends AbstractOrder<I>,
                                       I extends AbstractItem>
extends               AbstractContent {


    private final String firstName;
    private final String lastName;
    private final List<LazyRelation<T>> lazyOrders;
    @DBRef(lazy = true)
    private final List<T>               orders;

    public AbstractCustomer(Long id, String firstName, String lastName, List<LazyRelation<T>> lazyOrders, List<T> orders) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.lazyOrders = lazyOrders;
        this.orders = orders;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<LazyRelation<T>> getLazyOrders() {
        return lazyOrders;
    }
    
    public List<T> getOrders() {
        return orders;
    }
    
    @Override
    public String toString() {
        return String.format(
                "%s[id=%d, firstName='%s', lastName='%s', lazyOrders='%s', orders='%s']", 
                this.getClass().getSimpleName(), id, firstName, lastName, lazyOrders, orders);
    }    
}