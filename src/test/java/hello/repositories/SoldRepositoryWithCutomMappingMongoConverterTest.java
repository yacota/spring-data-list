package hello.repositories;

import hello.Application;
import hello.MongoEmbeddedConfiguration;
import hello.model.Item;
import hello.model.Order;
import hello.model.Sold;
import hello.model.generic.LazyRelation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import proposal.MongoDbConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MongoEmbeddedConfiguration.class, MongoDbConfiguration.class, Application.class })
public class SoldRepositoryWithCutomMappingMongoConverterTest {
    
    @Autowired
    private SoldRepository soldRepository;
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Reading from a list is correctly map into a single object
     */
    @Test
    public void testFindOneGeneric() {
        Sold findOne = soldRepository.findOne(1L);
        List<LazyRelation<Item>> items = findOne.getLazyOrder().getContent().getItems();
        Assert.assertNotNull(items);
    }
    
    @Test
    public void testFindOneNonGeneric() {
        Sold findOne = soldRepository.findOne(2L);
        List<LazyRelation<Item>> items = findOne.getOrder().getItems();
        Assert.assertNotNull(items);
    }
    
    /**
     * Creating as a single object is still correctly mapped as usual
     */
    @Test
    public void testCreateAndFind() {
        Order order = orderRepository.findOne(2L);
        Map<String, Object> atts = new HashMap();
        atts.put("someKey", "someValue");
        LazyRelation<Order> lr = new LazyRelation(atts);
        lr.setContent(order);
        Sold sold = new Sold(3L, order, lr);
        soldRepository.save(sold);
        Sold findOne = soldRepository.findOne(3L);
        Assert.assertEquals(order.getOrderDate(), findOne.getLazyOrder().getContent().getOrderDate());
        Assert.assertEquals("someValue", findOne.getLazyOrder().getAttributes().get("someKey"));
        Assert.assertEquals(order.getOrderDate(), findOne.getOrder().getOrderDate());
    }

}