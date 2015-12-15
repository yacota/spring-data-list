package hello.repositories;

import hello.Application;
import hello.MongoEmbeddedConfiguration;
import hello.model.Customer;
import static hello.repositories.CustomerRepositoryTest.ensureOrder1Data;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.convert.CustomMappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import proposal.MongoDbConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MongoEmbeddedConfiguration.class, MongoDbConfiguration.class, Application.class})
public class CustomerRepositoryWithCustomMappingConverterTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private MappingMongoConverter converter;
    
    @Test
    public void testSetUp() {
        Assert.assertTrue(CustomMappingMongoConverter.class.equals(converter.getClass()));
    }

    @Test
    public void testFindOne() {
        Customer findOne = customerRepository.findOne(1L);
        CustomerRepositoryTest.ensureOrder1Data(findOne.getLazyOrders().get(0).getContent());
        CustomerRepositoryTest.ensureOrder2Data(findOne.getLazyOrders().get(1).getContent());
        CustomerRepositoryTest.ensureOrder1Data(findOne.getOrders().get(0));
        CustomerRepositoryTest.ensureOrder2Data(findOne.getOrders().get(1));
    }
}