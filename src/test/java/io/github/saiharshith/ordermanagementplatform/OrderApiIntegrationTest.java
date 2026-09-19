package io.github.saiharshith.ordermanagementplatform;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Test
    void registerUser_thenDuplicateUsername_returnsConflict() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, "password123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, "password123")))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withCorrectCredentials_returnsToken_andWithWrongPassword_returnsUnauthorized() throws Exception {
        String username = uniqueUsername();
        register(username, "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrders_withoutAuthHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_missingCustomerName_returnsBadRequestWithFieldError() throws Exception {
        String token = registerAndLogin(uniqueUsername(), "password123");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(null, "Widget", 2, "CREATED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName").exists());
    }

    @Test
    void regularUser_canCreateAndReadOwnOrder_butCannotUpdateOrDelete() throws Exception {
        String token = registerAndLogin(uniqueUsername(), "password123");

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Alice", "Widget", 2, "CREATED")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Bob", "Gadget", 1, "SHIPPED")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canUpdateAndDeleteOrder_andOrderIsGoneAfterDelete() throws Exception {
        String adminToken = login(adminUsername, adminPassword);

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Alice", "Widget", 2, "CREATED")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Bob", "Gadget", 5, "SHIPPED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Bob"));

        mockMvc.perform(delete("/api/orders/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private String uniqueUsername() {
        return "user-" + UUID.randomUUID();
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, password)))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authJson(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asString();
    }

    private String registerAndLogin(String username, String password) throws Exception {
        register(username, password);
        return login(username, password);
    }

    private String authJson(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String orderJson(String customerName, String item, int quantity, String status) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (customerName != null) {
            body.put("customerName", customerName);
        }
        body.put("item", item);
        body.put("quantity", quantity);
        if (status != null) {
            body.put("status", status);
        }
        return objectMapper.writeValueAsString(body);
    }
}
