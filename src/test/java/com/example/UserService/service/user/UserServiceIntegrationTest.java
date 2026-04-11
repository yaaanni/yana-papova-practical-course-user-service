package com.example.UserService.service.user;

import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.entities.User;
import com.example.UserService.repository.UserRepository;
import com.example.UserService.service.utils.JwtServiceTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;


import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
@AutoConfigureMockMvc
class UserServiceIntegrationTest {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JwtServiceTest jwtServiceTest;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());


    @Container
    private static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:9.6.12")
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void create_shouldCreateUser() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult result = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andReturn();


        UserResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponse.class
        );

        assertNotNull(response.getId());

        Optional<User> fromDb = userRepository.findById(response.getId());
        assertTrue(fromDb.isPresent());
    }

    @Test
    void create_shouldFailWhenEmailAlreadyExists() throws Exception {
        String json = """
                {
                    "name": "New",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        mockMvc.perform(
                post("/users")
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        ).andExpect(status().isCreated());

        mockMvc.perform(
                post("/users")
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_shouldReturnUserWithHisCards() throws Exception {
        String json = """
                {
                    "name": "New",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        MvcResult result = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = response.getId();

        String cardJson = String.format("""
                {
                    "number": "1234567891234567",
                    "holder": "Person",
                    "expirationDate": "2036-03-04",
                    "userId": %d
                }
                """, id);

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated());

        MvcResult resultWithCard = mockMvc.perform(
                        get("/users/" + id)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = resultWithCard.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);

        assertEquals(id, root.get("id").asLong());

        JsonNode cards = root.get("cards");
        assertNotNull(cards);
        assertEquals(1, cards.size());

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);
    }

    @Test
    void getUserByEmail_shouldReturnUser() throws Exception {
        String json = """
        {
            "name": "New",
            "surname": "Person",
            "birthDate": "2006-03-04",
            "email": "new@example.com"
        }
        """;

        MvcResult result = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                UserResponse.class
        );

        String email = response.getEmail();

        MvcResult resultUser = mockMvc.perform(
                        get("/users/email/" + email)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = resultUser.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);

        assertEquals(email, root.get("email").asText());

        mockMvc.perform(
                        get("/users/email/" + email)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void update_shouldUpdateUser_whenUserIsExists() throws Exception {
        String json = """
                {
                    "name": "Old",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserResponse.class);

        Long id = created.getId();

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        String updateJson = """
                {
                    "name": "New",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        mockMvc.perform(put("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        Optional<User> fromDb = userRepository.findById(id);
        assertTrue(fromDb.isPresent());
        assertEquals("New", fromDb.get().getName());
    }

    @Test
    void update_shouldThrowNotFound_whenUserDoesNotExist() throws Exception {
        Long id = 1L;

        String updateJson = """
                {
                    "name": "New",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        mockMvc.perform(put("/users/" + id)
                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        ).andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnPagesWithUsers() throws Exception {
        String firstJson = """
                {
                    "name": "First",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "first@example.com"
                }
                """;
        mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstJson))
                .andExpect(status().isCreated()
                );
        String secondJson = """
                {
                    "name": "Second",
                    "surname": "Human",
                    "birthDate": "2006-03-04",
                    "email": "second@example.com"
                }
                """;
        mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondJson))
                .andExpect(status().isCreated()
                );

        MvcResult result = mockMvc.perform(
                        get("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("name", "First")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = root.get("content");

        assertEquals(1, content.size());
        assertEquals("First", content.get(0).get("name").asText());

        MvcResult result2 = mockMvc.perform(
                        get("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("surname", "Human")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        JsonNode content2 = root2.get("content");

        assertEquals(1, content2.size());
        assertEquals("Second", content2.get(0).get("name").asText());

        MvcResult result3 = mockMvc.perform(
                        get("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("name", "First")
                                .param("surname", "Person")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root3 = objectMapper.readTree(result3.getResponse().getContentAsString());
        JsonNode content3 = root3.get("content");

        assertEquals(1, content3.size());
        assertEquals("First", content3.get(0).get("name").asText());

        MvcResult result4 = mockMvc.perform(
                        get("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root4 = objectMapper.readTree(result4.getResponse().getContentAsString());
        JsonNode content4 = root4.get("content");

        assertEquals(2, content4.size());
    }

    @Test
    void activate_shouldActivateUser_andEvictCache() throws Exception {
        String json = """
                {
                    "name": "Test",
                    "surname": "User",
                    "birthDate": "2000-01-01",
                    "email": "test@example.com"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserResponse.class
        );
        Long id = created.getId();

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        mockMvc.perform(
                patch("/users/" + id + "/activate")
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        Optional<User> fromDb = userRepository.findById(id);
        assertTrue(fromDb.isPresent());
        assertTrue(fromDb.get().isActive());
    }

    @Test
    void deactivate_shouldDeactivateUser_andEvictCache() throws Exception {
        String json = """
                {
                    "name": "Test",
                    "surname": "User",
                    "birthDate": "2000-01-01",
                    "email": "test@example.com"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserResponse.class
        );
        Long id = created.getId();

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        mockMvc.perform(
                patch("/users/" + id + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
        ).andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        Optional<User> fromDb = userRepository.findById(id);
        assertTrue(fromDb.isPresent());
        assertFalse(fromDb.get().isActive());
    }

    @Test
    void delete_shouldRemoveUser_andEvictCache() throws Exception {
        String json = """
                {
                    "name": "Test",
                    "surname": "User",
                    "birthDate": "2000-01-01",
                    "email": "test@example.com"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                UserResponse.class
        );
        Long id = created.getId();

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        mockMvc.perform(
                delete("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        Optional<User> fromDb = userRepository.findById(id);
        assertTrue(fromDb.isEmpty());
    }
}
