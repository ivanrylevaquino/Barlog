package com.barlog.barlog_back.journal;

import com.barlog.barlog_back.auth.Role;
import com.barlog.barlog_back.auth.User;
import com.barlog.barlog_back.auth.UserRepository;
import com.barlog.barlog_back.security.JwtTokenProvider;
import com.barlog.barlog_back.workout.WorkoutSessionRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class JournalControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired JournalEntryRepository journals;
    @Autowired JwtTokenProvider jwt;
    @Autowired com.barlog.barlog_back.video.VideoClipRepository videoClips;

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

    /** Starts a session with one push-up set of 10 reps; finishes it when asked. */
    private String session(boolean finish) throws Exception {
        long ex = body(as(tokenA, get("/api/exercises")), 200).get(0).get("id").asLong();
        String sid = body(as(tokenA, post("/api/sessions")).content("{}"), 201).get("id").asText();
        body(as(tokenA, post("/api/sessions/" + sid + "/sets"))
                .content("{\"exerciseId\":" + ex + ",\"progression\":\"Standard\",\"reps\":10}"), 201);
        if (finish) body(as(tokenA, patch("/api/sessions/" + sid + "/finish")), 200);
        return sid;
    }

    private static final String ENTRY = "{\"mood\":\"GOOD\",\"energy\":4,\"tags\":[\"STRONG\",\"SORE\"],"
            + "\"body\":\"  Last set felt heavy but I kept my form.  \"}";

    @Test
    void writeReadUpdateDelete() throws Exception {
        String sid = session(true);

        body(as(tokenA, get("/api/sessions/" + sid + "/journal")), 204);

        mvc.perform(as(tokenA, put("/api/sessions/" + sid + "/journal")).content(ENTRY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mood").value("GOOD"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.body").value("Last set felt heavy but I kept my form."))
                .andExpect(jsonPath("$.recap.reps").value(10))
                .andExpect(jsonPath("$.recap.sets").value(1));

        // PUT again updates the same entry instead of creating a second one
        body(as(tokenA, put("/api/sessions/" + sid + "/journal")).content("{\"mood\":\"GREAT\"}"), 200);
        mvc.perform(as(tokenA, get("/api/journal")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mood").value("GREAT"))
                .andExpect(jsonPath("$[0].tags.length()").value(0));

        // mood shows up on the session for the History page
        mvc.perform(as(tokenA, get("/api/sessions"))).andExpect(jsonPath("$[0].mood").value("GREAT"));

        body(as(tokenA, delete("/api/sessions/" + sid + "/journal")), 204);
        body(as(tokenA, get("/api/sessions/" + sid + "/journal")), 204);
    }

    @Test
    void rulesAndValidation() throws Exception {
        String open = session(false);
        body(as(tokenA, put("/api/sessions/" + open + "/journal")).content(ENTRY), 409);
        body(as(tokenA, patch("/api/sessions/" + open + "/finish")), 200);

        body(as(tokenA, put("/api/sessions/" + open + "/journal")).content("{}"), 400);
        body(as(tokenA, put("/api/sessions/" + open + "/journal")).content("{\"mood\":\"GOOD\",\"energy\":6}"), 400);
        body(as(tokenA, put("/api/sessions/" + open + "/journal")).content("{\"mood\":\"HAPPY\"}"), 400);
        body(as(tokenA, put("/api/sessions/" + open + "/journal")).content("{\"mood\":\"GOOD\",\"body\":\"" + "x".repeat(5001) + "\"}"), 400);
    }

    @Test
    void privateToOwnerAndRemovedWithSession() throws Exception {
        String sid = session(true);
        body(as(tokenA, put("/api/sessions/" + sid + "/journal")).content(ENTRY), 200);

        body(as(tokenB, get("/api/sessions/" + sid + "/journal")), 404);
        body(as(tokenB, put("/api/sessions/" + sid + "/journal")).content(ENTRY), 404);
        mvc.perform(as(tokenB, get("/api/journal"))).andExpect(jsonPath("$.length()").value(0));

        body(as(tokenA, delete("/api/sessions/" + sid)), 204);
        mvc.perform(as(tokenA, get("/api/journal"))).andExpect(jsonPath("$.length()").value(0));
    }
}
