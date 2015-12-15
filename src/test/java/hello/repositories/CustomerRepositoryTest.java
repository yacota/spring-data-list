package hello.repositories;

import hello.Application;
import hello.MongoEmbeddedConfiguration;
import hello.model.Customer;
import hello.model.Item;
import hello.model.Order;
import hello.model.generic.LazyRelation;
import java.util.List;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MongoEmbeddedConfiguration.class, Application.class})
public class CustomerRepositoryTest {
    
    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void testFindOne() {
        Customer findOne = customerRepository.findOne(1L);
        ensureOrder1Data(findOne.getLazyOrders().get(0).getContent());
        ensureOrder2Data(findOne.getLazyOrders().get(1).getContent());
        ensureOrder1Data(findOne.getOrders().get(0));
        ensureOrder2Data(findOne.getOrders().get(1));
    }
    
    
    static void ensureOrder1Data(Order order1) {
        List<LazyRelation<Item>> items = order1.getItems();
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("itemName1", items.get(0).getContent().getName());
        Assert.assertEquals("itemName2", items.get(1).getContent().getName());
        
    }
    
    static  void ensureOrder2Data(Order order2) {
        List<LazyRelation<Item>> items = order2.getItems();
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("itemName3", items.get(0).getContent().getName());
        Assert.assertEquals("itemName4", items.get(1).getContent().getName());
    }
}