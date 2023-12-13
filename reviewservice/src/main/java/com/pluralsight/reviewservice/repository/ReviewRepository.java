package com.pluralsight.reviewservice.repository;

import java.util.Optional;

import com.pluralsight.reviewservice.model.Review;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByProductId(Integer productId);
}
