package com.globomantics.productservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.database.rider.core.api.connection.ConnectionHolder;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import com.globomantics.productservice.model.Product;
import com.globomantics.productservice.repository.ProductRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Optional;

@ExtendWith({DBUnitExtension.class, SpringExtension.class})
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductServiceIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ConnectionHolder getConnectionHolder() {
        // Return a function that retrieves a connection from our data source
        return () -> dataSource.getConnection();
    }

    @Test
    @DisplayName("GET /product/100 - Found")
    @DataSet("products.yml")
    void testGetProductByIdFound() throws Exception {
        // Execute the GET request
        mockMvc.perform(get("/product/{id}", 100))

                // Validate the response code and content type
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate the headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().string(HttpHeaders.LOCATION, "/product/100"))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is("Product 1")))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("GET /product/99 - Not Found")
    @DataSet("products.yml")
    void testGetProductByIdNotFound() throws Exception {
        // Execute the GET request
        mockMvc.perform(get("/product/{id}", 99))

                // Validate that we get a 404 Not Found response
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /product - Success")
    @DataSet("products.yml")
    void testCreateProduct() throws Exception {
        // Setup product to create
        Product postProduct = new Product("Product Name", 10);

        mockMvc.perform(post("/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(postProduct)))

                // Validate the response code and content type
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate the headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().exists(HttpHeaders.LOCATION))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", any(Integer.class)))
                .andExpect(jsonPath("$.name", is("Product Name")))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.version", is(1)));

        // Validate that the new product is in the database
        Optional<Product> productInDatabase = findProductByName("Product Name");
        Assertions.assertTrue(productInDatabase.isPresent(), "New product should be in the database");
        Assertions.assertEquals("Product Name", productInDatabase.get().getName(), "The name of the product should be Product Name");
        Assertions.assertEquals(10, productInDatabase.get().getQuantity(), "The product quantity should be 10");
        Assertions.assertEquals(1, productInDatabase.get().getVersion(), "The product version should be 1");
    }

    @Test
    @DisplayName("PUT /product/200 - Success")
    @DataSet("products.yml")
    void testProductPutSuccess() throws Exception {
        // Setup product to update
        Product putProduct = new Product("Product 200 Updated", 10);

        mockMvc.perform(put("/product/{id}", 200)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.IF_MATCH, 2)
                .content(asJsonString(putProduct)))

                // Validate the response code and content type
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // Validate the headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"3\""))
                .andExpect(header().string(HttpHeaders.LOCATION, "/product/200"))

                // Validate the returned fields
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.name", is("Product 200 Updated")))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.version", is(3)));

        // Validate the product in the database
        Optional<Product> productInDatabase = findProductById(200);
        Assertions.assertTrue(productInDatabase.isPresent(), "Product should be in the database");
        Assertions.assertEquals(200, productInDatabase.get().getId(), "The product ID should be 200");
        Assertions.assertEquals("Product 200 Updated", productInDatabase.get().getName(), "The name of the product should be Product 200 Updated");
        Assertions.assertEquals(10, productInDatabase.get().getQuantity(), "The product quantity should be 10");
        Assertions.assertEquals(3, productInDatabase.get().getVersion(), "The product version should be 3");
    }

    @Test
    @DisplayName("PUT /product/100 - Version Mismatch")
    @DataSet("products.yml")
    void testProductPutVersionMismatch() throws Exception {
        // Setup product to update
        Product putProduct = new Product("Product Name", 10);

        mockMvc.perform(put("/product/{id}", 100)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.IF_MATCH, 7)
                .content(asJsonString(putProduct)))

                // Validate the response code and content type
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /product/99 - Not Found")
    @DataSet("products.yml")
    void testProductPutNotFound() throws Exception {
        // Setup product to update
        Product putProduct = new Product("Product Name", 10);

        mockMvc.perform(put("/product/{id}", 99)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.IF_MATCH, 1)
                .content(asJsonString(putProduct)))

                // Validate the response code and content type
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /product/100 - Success")
    @DataSet("products.yml")
    void testProductDeleteSuccess() throws Exception {
        // Execute our DELETE request
        mockMvc.perform(delete("/product/{id}", 100))
                .andExpect(status().isOk());

        // Validate that the product was deleted from the database
        Optional<Product> productInDatabase = findProductById(100);
        Assertions.assertFalse(productInDatabase.isPresent());
    }

    @Test
    @DisplayName("DELETE /product/99 - Not Found")
    @DataSet("products.yml")
    void testProductDeleteNotFound() throws Exception {
        // Execute our DELETE request
        mockMvc.perform(delete("/product/{id}", 99))
                .andExpect(status().isNotFound());
    }

    static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method that finds a product by its name.
     * @param productName   The name of the product to query for
     * @return              The product wrapped in an Optional, or Optional.empty() if it is not found
     */
    private Optional<Product> findProductByName(String productName) {
        try {
            Product product = jdbcTemplate.queryForObject("SELECT * FROM products WHERE name = ?",
                    (rs, rowNum) -> {
                        Product p = new Product();
                        p.setId(rs.getInt("id"));
                        p.setName(rs.getString("name"));
                        p.setQuantity(rs.getInt("quantity"));
                        p.setVersion(rs.getInt("version"));
                        return p;
                    },
                    productName);
            return Optional.of(product);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Helper method that finds a product by its id.
     * @param id    The id of the product to query for
     * @return      The product wrapped in an Optional, or Optional.empty() if it is not found
     */
    private Optional<Product> findProductById(Integer id) {
        try {
            Product product = jdbcTemplate.queryForObject("SELECT * FROM products WHERE id = ?",
                    (rs, rowNum) -> {
                        Product p = new Product();
                        p.setId(rs.getInt("id"));
                        p.setName(rs.getString("name"));
                        p.setQuantity(rs.getInt("quantity"));
                        p.setVersion(rs.getInt("version"));
                        return p;
                    },
                    id);
            return Optional.of(product);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
