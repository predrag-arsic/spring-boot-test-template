package com.pluralsight.reviewservice.integration;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pluralsight.reviewservice.model.Review;
import com.pluralsight.reviewservice.model.ReviewEntry;
import com.pluralsight.reviewservice.repository.MongoDataFile;
import com.pluralsight.reviewservice.repository.MongoSpringExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith({SpringExtension.class, MongoSpringExtension.class})
@SpringBootTest
@AutoConfigureMockMvc
public class ReviewServiceIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * MongoSpringExtension method that returns the autowired MongoTemplate to use for MongoDB interactions.
     *
     * @return The autowired MongoTemplate instance.
     */
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Test
    @DisplayName("GET /review/1 - Found")
    @MongoDataFile(value = "sample.json", classType = Review.class, collectionName = "Reviews")
    void testGetReviewByIdFound() throws Exception {

        // Execute the GET request
        mockMvc.perform(get("/review/{id}", 1))

                // Validate the response code and content type
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate the headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().string(HttpHeaders.LOCATION, "/review/1"))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.entries.length()", is(1)))
                .andExpect(jsonPath("$.entries[0].username", is("user1")))
                .andExpect(jsonPath("$.entries[0].review", is("This is a review")))
                .andExpect(jsonPath("$.entries[0].date", is("2023-10-10T11:38:26.855+00:00")));
    }

    @Test
    @DisplayName("GET /review/99 - Not Found")
    @MongoDataFile(value = "sample.json", classType = Review.class, collectionName = "Reviews")
    void testGetReviewByIdNotFound() throws Exception {

        // Execute the GET request
        mockMvc.perform(get("/review/{id}", 99))

                // Validate that we get a 404 Not Found response
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /review - Success")
    @MongoDataFile(value = "sample.json", classType = Review.class, collectionName = "Reviews")
    void testCreateReview() throws Exception {
        // Create a Review to POST to the controller
        Review postReview = new Review(1);
        postReview.getEntries().add(new ReviewEntry("test-user", "Great product"));

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(postReview)))

                // Validate the response code and content type
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate the headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().exists(HttpHeaders.LOCATION))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", any(String.class)))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.entries.length()", is(1)))
                .andExpect(jsonPath("$.entries[0].username", is("test-user")))
                .andExpect(jsonPath("$.entries[0].review", is("Great product")))
                .andExpect(jsonPath("$.entries[0].date", any(String.class)));

        // Validate the review in MongoDB
        Review reviewInMongo = findReviewByProductId(1);
        Assertions.assertNotNull(reviewInMongo, "Review should not be null");
        Assertions.assertEquals(1, reviewInMongo.getProductId(), "Product ID should be 1");
        Assertions.assertEquals(1, reviewInMongo.getVersion(), "Review version should be 1");
        Assertions.assertEquals(1, reviewInMongo.getEntries().size(), "There should be 1 review entry");
    }

    @Test
    @DisplayName("POST /review/{productId}/entry")
    @MongoDataFile(value = "sample.json", classType = Review.class, collectionName = "Reviews")
    void testAddEntryToReview() throws Exception {
        // Create a ReviewEntry to add to the review
        ReviewEntry reviewEntry = new ReviewEntry("test-user", "Great product");

        mockMvc.perform(post("/review/{productId}/entry", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(reviewEntry)))

                // Validate the response code and content type
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().string(HttpHeaders.LOCATION, "/review/1"))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.entries.length()", is(2)))
                .andExpect(jsonPath("$.entries[0].username", is("user1")))
                .andExpect(jsonPath("$.entries[0].review", is("This is a review")))
                .andExpect(jsonPath("$.entries[0].date", is("2023-10-10T11:38:26.855+00:00")))
                .andExpect(jsonPath("$.entries[1].username", is("test-user")))
                .andExpect(jsonPath("$.entries[1].review", is("Great product")))
                .andExpect(jsonPath("$.entries[1].date", any(String.class)));

        // Validate the review in MongoDB
        Review reviewInMongo = findReviewById("1");
        Assertions.assertNotNull(reviewInMongo, "Review should not be null");
        Assertions.assertEquals(1, reviewInMongo.getProductId(), "Product ID should be 1");
        Assertions.assertEquals(1, reviewInMongo.getVersion(), "Product ID should be 1");
        Assertions.assertEquals(2, reviewInMongo.getEntries().size(), "There should be 2 review entries");
    }

    private Review findReviewByProductId(Integer productId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("productId").is(productId));
        return mongoTemplate.findOne(query, Review.class, "Reviews");
    }

    private Review findReviewById(String id) {
        return mongoTemplate.findById(id, Review.class);
    }

    static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
