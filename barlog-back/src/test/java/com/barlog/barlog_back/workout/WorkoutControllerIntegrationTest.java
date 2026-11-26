package com.barlog.barlog_back.workout;

import com.barlog.barlog_back.auth.Role;
import com.barlog.barlog_back.auth.User;
import com.barlog.barlog_back.auth.UserRepository;
import com.barlog.barlog_back.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests of the workout logging API against the H2 test database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class WorkoutControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired JwtTokenProvider jwt;
    @Autowired com.barlog.barlog_back.video.VideoClipRepository videoClips;
    @Autowired com.barlog.barlog_back.journal.JournalEntryRepository journals;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        videoClips.deleteAll();
        journals.deleteAll();
        sessions.deleteAll();
        tokenA = "Bearer " + jwt.generateToken(newUser("a"));
        tokenB = "Bearer " + jwt.generateToken(newUser("b"));
    }

    private User newUser(String tag) {
        String id = tag + "-" + UUID.randomUUID();
        return users.save(User.builder().googleId("g-" + id).email(id + "@barlog.test")
                .displayName("User " + tag).role(Role.PRACTITIONER).build());
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder b) {
        return b.header("Authorization", token).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode body(MockHttpServletRequestBuilder b, int status) throws Exception {
        String s = mvc.perform(b).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return s.isEmpty() ? null : json.readTree(s);
    }

    private long pushUpId() throws Exception {
        for (JsonNode e : body(as(tokenA, get("/api/exercises")), 200)) {
            if (e.get("type").asText().equals("PUSH_UP")) return e.get("id").asLong();
        }
        throw new AssertionError("Push-up not seeded");
    }

    private String set(long exId, String prog, int reps) {
        return "{\"exerciseId\":" + exId + ",\"progression\":\"" + prog + "\",\"reps\":" + reps + ",\"weightKg\":0,\"rpe\":7,\"notes\":\"ok\"}";
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogueIsSeeded() throws Exception {
        mvc.perform(as(tokenA, get("/api/exercises")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].progressions").isArray());
    }

    @Test
    void fullWorkoutFlow() throws Exception {
        long ex = pushUpId();

        JsonNode s = body(as(tokenA, post("/api/sessions")).content("{\"notes\":\"Upper day\"}"), 201);
        String sid = s.get("id").asText();

        // only one active session at a time
        body(as(tokenA, post("/api/sessions")).content("{}"), 409);
        // cannot finish an empty session
        body(as(tokenA, patch("/api/sessions/" + sid + "/finish")), 409);

        JsonNode s1 = body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Standard", 10)), 201);
        JsonNode s2 = body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "diamond", 6)), 201);
        assertEquals(1, s1.get("setNumber").asInt());
        assertEquals(2, s2.get("setNumber").asInt());
        assertEquals("Diamond", s2.get("progression").asText());

        // update + read back
        body(as(tokenA, patch("/api/sets/" + s1.get("id").asText())).content(set(ex, "Standard", 12)), 200);
        mvc.perform(as(tokenA, get("/api/sets/" + s1.get("id").asText())))
                .andExpect(jsonPath("$.reps").value(12));

        // delete first set renumbers the second
        body(as(tokenA, delete("/api/sets/" + s1.get("id").asText())), 204);
        mvc.perform(as(tokenA, get("/api/sessions/" + sid)))
                .andExpect(jsonPath("$.sets.length()").value(1))
                .andExpect(jsonPath("$.sets[0].setNumber").value(1));

        mvc.perform(as(tokenA, patch("/api/sessions/" + sid + "/finish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
        // finished sessions are closed to new sets
        body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Standard", 5)), 409);

        mvc.perform(as(tokenA, get("/api/progress?range=7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.volume").value(6))
                .andExpect(jsonPath("$.totals.maxReps").value(6))
                .andExpect(jsonPath("$.totals.sessions").value(1))
                .andExpect(jsonPath("$.points.length()").value(1));

        mvc.perform(as(tokenA, get("/api/sessions")))
                .andExpect(jsonPath("$.length()").value(1));

        body(as(tokenA, delete("/api/sessions/" + sid)), 204);
        body(as(tokenA, get("/api/sessions/" + sid)), 404);
    }

    @Test
    void validationErrors() throws Exception {
        long ex = pushUpId();
        String sid = body(as(tokenA, post("/api/sessions")).content("{}"), 201).get("id").asText();

        body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Standard", 0)), 400);
        body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Muscle-up", 5)), 400);
        body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + ex + ",\"progression\":\"Standard\",\"reps\":5,\"rpe\":11}"), 400);
        body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + ex + ",\"progression\":\"Standard\",\"reps\":5,\"weightKg\":-1}"), 400);
        body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(99999, "Standard", 5)), 404);
        body(as(tokenA, get("/api/progress?range=15")), 400);
        body(as(tokenA, get("/api/sessions/not-a-uuid")), 400);
    }

    @Test
    void setsTimesRepsHoldsAndCorrectingTheExercise() throws Exception {
        long push = pushUpId();
        long plank = -1;
        for (JsonNode e : body(as(tokenA, get("/api/exercises")), 200)) {
            if (e.get("type").asText().equals("PLANK")) plank = e.get("id").asLong();
        }
        String sid = body(as(tokenA, post("/api/sessions")).content("{}"), 201).get("id").asText();

        JsonNode entry = body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + push + ",\"progression\":\"Standard\",\"reps\":10,\"sets\":3}"), 201);
        assertEquals(3, entry.get("sets").asInt());

        // a hold needs seconds, not reps
        body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + plank + ",\"progression\":\"Forearm\"}"), 400);
        JsonNode hold = body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + plank + ",\"progression\":\"Forearm\",\"holdSeconds\":45,\"sets\":2}"), 201);
        assertEquals(45, hold.get("holdSeconds").asInt());
        assertTrue(hold.get("hold").asBoolean());

        // correcting a wrong exercise (e.g. after auto-detection) is allowed
        body(as(tokenA, patch("/api/sets/" + hold.get("id").asText()))
                .content("{\"exerciseId\":" + push + ",\"progression\":\"Incline\",\"reps\":12}"), 200);

        // volume counts every set: 3x10 + 1x12
        mvc.perform(as(tokenA, get("/api/progress?range=7"))).andExpect(jsonPath("$.totals.volume").value(42));
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        long ex = pushUpId();
        String sid = body(as(tokenA, post("/api/sessions")).content("{}"), 201).get("id").asText();
        String setId = body(as(tokenA, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Standard", 8)), 201).get("id").asText();

        body(as(tokenB, get("/api/sessions/" + sid)), 404);
        body(as(tokenB, get("/api/sets/" + setId)), 404);
        body(as(tokenB, delete("/api/sessions/" + sid)), 404);
        body(as(tokenB, post("/api/sessions/" + sid + "/sets")).content(set(ex, "Standard", 8)), 404);
        mvc.perform(as(tokenB, get("/api/sessions"))).andExpect(jsonPath("$.length()").value(0));
    }
}
