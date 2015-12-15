package hello.repositories;

import hello.model.Sold;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SoldRepository extends MongoRepository<Sold, Long> {

}
