package com.example.UserService.service.card;

import com.example.UserService.dto.card.CardResponse;
import com.example.UserService.dto.user.UserByIdResponse;
import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.entities.Card;
import com.example.UserService.repository.CardRepository;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Testcontainers
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CardServiceIntegrationTest {
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

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
    void create_shouldCreateUser_whenUserIsExists() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        UserByIdResponse cached = cache.get(id, UserByIdResponse.class);
        assertNotNull(cached);
        assertEquals(userResponse.getId(), cached.getId());
        assertEquals(userResponse.getName(), cached.getName());

        String cardJson = String.format("""
                    {
                        "number": "1234567891234567",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        assertNotNull(cardResponse.getId());
        assertTrue(cardRepository.findById(cardResponse.getId()).isPresent());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);
    }


    @Test
    void create_shouldThrowUserNotFound_whenUserDoesNotExist() throws Exception {
        Long id = 1L;

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
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldThrowMoreThanFiveCards() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        String firstCardJson = String.format("""
                    {
                        "number": "1234567891234561",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String secondCardJson = String.format("""
                    {
                        "number": "1234567891234562",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String thirdCardJson = String.format("""
                    {
                        "number": "1234567891234563",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String fourthCardJson = String.format("""
                    {
                        "number": "1234567891234564",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String fifthCardJson = String.format("""
                    {
                        "number": "1234567891234565",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String sixthCardJson = String.format("""
                    {
                        "number": "1234567891234566",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(thirdCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fourthCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fifthCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(sixthCardJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCardById_shouldReturnCard_whenCardIsExists() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        String cardJson = String.format("""
                    {
                        "number": "1234567891234567",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        Long cardId = cardResponse.getId();

        MvcResult cardResultById = mockMvc.perform(
                        get("/cards/" + cardId)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        CardResponse cardResponseById = objectMapper.readValue(
                cardResultById.getResponse().getContentAsString(),
                CardResponse.class
        );

        assertNotNull(cardResponseById);
        assertEquals(cardResponse.getId(), cardResponseById.getId());
    }

    @Test
    void getCardById_shouldThrowNotFound_whenCardDoesNotExist() throws Exception {
        Long id = 1L;

        mockMvc.perform(
                        get("/cards/" + id)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnPageWithCards() throws Exception {
        String firstJson = """
                    {
                        "name": "First",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult firstResultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstJson))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse firstUserResponse = objectMapper.readValue(
                firstResultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        String secondJson = """
                    {
                        "name": "Second",
                        "surname": "Human",
                        "birthDate": "2006-03-04",
                        "email": "new2@example.com"
                    }        
                """;


        MvcResult secondResultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondJson))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse secondUserResponse = objectMapper.readValue(
                secondResultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long firstId = firstUserResponse.getId();

        Long secondId = secondUserResponse.getId();

        String cardJson = String.format("""
                    {
                        "number": "1234567891234566",
                        "holder": "First",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, firstId);

        String secondCardJson = String.format("""
                    {
                        "number": "1234567891234567",
                        "holder": "Second",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, secondId);

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondCardJson))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        get("/cards")
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
        assertEquals("First", content.get(0).get("holder").asText());

        MvcResult result2 = mockMvc.perform(
                        get("/cards")
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
        assertEquals("Second", content2.get(0).get("holder").asText());

        MvcResult result3 = mockMvc.perform(
                        get("/cards")
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
        assertEquals("First", content3.get(0).get("holder").asText());

        MvcResult result4 = mockMvc.perform(
                        get("/cards")
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
    void getAllCardsByUserId_shouldReturnCards_whenUserIsExists() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        String firstCardJson = String.format("""
                    {
                        "number": "1234567891234561",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        String secondCardJson = String.format("""
                    {
                        "number": "1234567891234562",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstCardJson))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondCardJson))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        get("/cards/" + id + "/all")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(root.isArray());

        assertEquals(2, root.size());

        List<String> numbers = new ArrayList<>();
        root.forEach(node -> numbers.add(node.get("number").asText()));

        assertTrue(numbers.contains("1234567891234561"));
        assertTrue(numbers.contains("1234567891234562"));
    }

    @Test
    void activate_shouldSwitchActive_whenCardIsExists() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        UserByIdResponse cached = cache.get(id, UserByIdResponse.class);
        assertNotNull(cached);
        assertEquals(userResponse.getId(), cached.getId());
        assertEquals(userResponse.getName(), cached.getName());

        String cardJson = String.format("""
                    {
                        "number": "1234567891234567",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        assertTrue(cardResponse.getActive());

        Long cardId = cardResponse.getId();
        assertNotNull(cardId);

        Optional<Card> fromDb = cardRepository.findById(cardId);
        assertTrue(fromDb.isPresent());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        cached = cache.get(id, UserByIdResponse.class);
        assertNotNull(cached);
        assertEquals(userResponse.getId(), cached.getId());
        assertEquals(userResponse.getName(), cached.getName());

        mockMvc.perform(
                        patch("/cards/" + cardId + "/activate")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isOk());

        CardResponse cardResponseAfterActive = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        MvcResult afterActivateResult = mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        CardResponse updatedCard = objectMapper.readValue(
                afterActivateResult.getResponse().getContentAsString(),
                CardResponse.class);

        assertTrue(updatedCard.getActive());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);
    }

    @Test
    void activate_shouldThrowNotFound_whenCardDoesNotExist() throws Exception {
        Long id = 1L;

        mockMvc.perform(
                        patch("/cards/" + id + "/deactivate")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivate_shouldSwitchActive_whenCardIsExists() throws Exception {
        String json = """
                    {
                        "name": "New",
                        "surname": "Person",
                        "birthDate": "2006-03-04",
                        "email": "new@example.com"
                    }        
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long id = userResponse.getId();

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        UserByIdResponse cached = cache.get(id, UserByIdResponse.class);
        assertNotNull(cached);
        assertEquals(userResponse.getId(), cached.getId());
        assertEquals(userResponse.getName(), cached.getName());

        String cardJson = String.format("""
                    {
                        "number": "1234567891234567",
                        "holder": "Person",
                        "expirationDate": "2036-03-04",
                        "userId": %d
                    }        
                """, id);

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        assertTrue(cardResponse.getActive());

        Long cardId = cardResponse.getId();
        assertNotNull(cardId);

        Optional<Card> fromDb = cardRepository.findById(cardId);
        assertTrue(fromDb.isPresent());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);

        mockMvc.perform(get("/users/" + id)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk());

        await().atMost(1, SECONDS).until(() -> cache.get(id) != null);

        cached = cache.get(id, UserByIdResponse.class);
        assertNotNull(cached);
        assertEquals(userResponse.getId(), cached.getId());
        assertEquals(userResponse.getName(), cached.getName());

        mockMvc.perform(
                        patch("/cards/" + cardId + "/deactivate")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isOk());

        MvcResult afterDectivateResult = mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        CardResponse updatedCard = objectMapper.readValue(
                afterDectivateResult.getResponse().getContentAsString(),
                CardResponse.class);

        assertFalse(updatedCard.getActive());

        await().atMost(1, SECONDS).until(() -> cache.get(id) == null);
    }

    @Test
    void deactivate_shouldThrowNotFound_whenCardDoesNotExist() throws Exception {
        Long id = 1L;

        mockMvc.perform(
                        patch("/cards/" + id + "/deactivate")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldDeleteCard_whenCardIsExists() throws Exception {
        String json = """
                {
                    "name": "New",
                    "surname": "Person",
                    "birthDate": "2006-03-04",
                    "email": "new@example.com"
                }
                """;

        MvcResult resultUser = mockMvc.perform(
                        post("/users")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                resultUser.getResponse().getContentAsString(),
                UserResponse.class
        );

        Long userId = userResponse.getId();

        String cardJson = String.format("""
                {
                    "number": "1234567891234567",
                    "holder": "Person",
                    "expirationDate": "2036-03-04",
                    "userId": %d
                }
                """, userId);

        MvcResult cardResult = mockMvc.perform(
                        post("/cards")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardJson))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );

        Long cardId = cardResponse.getId();

        assertTrue(cardRepository.findById(cardId).isPresent());

        mockMvc.perform(delete("/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNotFound());

        assertFalse(cardRepository.findById(cardId).isPresent());

        Cache cache = cacheManager.getCache("users");
        assertNotNull(cache);

        await().atMost(1, SECONDS).until(() -> cache.get(userId) == null);
    }

    @Test
    void delete_shouldThrowNotFound_whenCardDoesNotExist() throws Exception {
        Long id = 1L;

        mockMvc.perform(
                        delete("/cards/" + id)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }
}