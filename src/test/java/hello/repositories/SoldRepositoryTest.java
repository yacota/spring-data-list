package hello.repositories;

import hello.Application;
import hello.MongoEmbeddedConfiguration;
import hello.model.Item;
import hello.model.Sold;
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
public class SoldRepositoryTest {
    
    @Autowired
    private SoldRepository soldRepository;

    @Test
    public void testFindOneGeneric() {
        Sold findOne = soldRepository.findOne(1L);
        // following line fails because of a single object mapped with generics
        List<LazyRelation<Item>> items = findOne.getLazyOrder().getContent().getItems();
        Assert.assertNotNull(items);
    }
    
    @Test
    public void testFindOneNonGeneric() {
        Sold findOne = soldRepository.findOne(2L);
        List<LazyRelation<Item>> items = findOne.getOrder().getItems();
        Assert.assertNotNull(items);
    }

}