package com.barlog.barlog_back.video;

import com.barlog.barlog_back.auth.Role;
import com.barlog.barlog_back.auth.User;
import com.barlog.barlog_back.auth.UserRepository;
import com.barlog.barlog_back.journal.JournalEntryRepository;
import com.barlog.barlog_back.security.JwtTokenProvider;
import com.barlog.barlog_back.workout.WorkoutSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Upload -> async analysis -> result on the set, with the Python service mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class VideoControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired JournalEntryRepository journals;
    @Autowired VideoClipRepository videos;
    @Autowired VideoStorage storage;
    @Autowired JwtTokenProvider jwt;
    @MockBean AnalysisClient analysis;

    static final String RESULT = """
            {"status":"COMPLETED","failureReason":null,"overall":"GOOD","repsDetected":8,"holdSeconds":null,
             "summary":"8 reps counted. Your form held up well.",
             "items":[{"type":"POSITIVE","t":"0:03","text":"Elbows reach about 90° at the bottom of your reps."}],
             "metrics":{}}""";

    private String tokenA, tokenB;

    @BeforeEach
    void setUp() {
        videos.deleteAll();
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
        return b.header("Authorization", token);
    }

    private JsonNode body(MockHttpServletRequestBuilder b, int status) throws Exception {
        String s = mvc.perform(b).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return s.isEmpty() ? null : json.readTree(s);
    }

    private String newSet() throws Exception {
        long ex = body(as(tokenA, get("/api/exercises")), 200).get(0).get("id").asLong();
        String sid = body(as(tokenA, post("/api/sessions")).contentType(MediaType.APPLICATION_JSON).content("{}"), 201).get("id").asText();
        return body(as(tokenA, post("/api/sessions/" + sid + "/sets")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"exerciseId\":" + ex + ",\"progression\":\"Standard\",\"reps\":8}"), 201).get("id").asText();
    }

    private MockMultipartFile mp4() {
        return new MockMultipartFile("file", "set.mp4", "video/mp4", new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p'});
    }

    private JsonNode waitForVideo(String setId, String wanted) throws Exception {
        for (int i = 0; i < 50; i++) {
            JsonNode v = body(as(tokenA, get("/api/sets/" + setId)), 200).get("video");
            if (v != null && wanted.equals(v.get("status").asText())) return v;
            Thread.sleep(100);
        }
        fail("Video never reached " + wanted);
        return null;
    }

    @Test
    void uploadAnalyzeStreamAndDelete() throws Exception {
        when(analysis.analyze(anyString(), eq("AUTO"), eq("PUSH_UP"))).thenReturn(RESULT);
        String setId = newSet();

        JsonNode up = body(as(tokenA, multipart("/api/sets/" + setId + "/video").file(mp4())), 202);
        assertEquals("PROCESSING", up.get("status").asText());

        JsonNode v = waitForVideo(setId, "COMPLETED");
        assertEquals(8, v.get("analysis").get("repsDetected").asInt());
        assertEquals("GOOD", v.get("analysis").get("overall").asText());

        // signed playback URL works without a JWT; a tampered one does not
        URI url = URI.create(v.get("url").asText());
        mvc.perform(get(url.getPath() + "?" + url.getQuery())).andExpect(status().isOk());
        mvc.perform(get(url.getPath() + "?" + url.getQuery().replaceAll("sig=.", "sig=X"))).andExpect(status().isForbidden());

        // shows up for Compare
        assertEquals(1, body(as(tokenA, get("/api/sets?hasVideo=true")), 200).size());

        // deleting the set removes the video row and file
        VideoClip clip = videos.findBySet_SetId(UUID.fromString(setId)).orElseThrow();
        assertTrue(Files.exists(storage.resolve(clip.getStorageKey())));
        body(as(tokenA, delete("/api/sets/" + setId)), 204);
        assertTrue(videos.findById(clip.getVideoId()).isEmpty());
        assertFalse(Files.exists(storage.resolve(clip.getStorageKey())));
    }

    @Test
    void analysisServiceDownMarksFailedThenRetryWorks() throws Exception {
        when(analysis.analyze(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Connection refused"));
        String setId = newSet();
        body(as(tokenA, multipart("/api/sets/" + setId + "/video").file(mp4())), 202);

        JsonNode v = waitForVideo(setId, "FAILED");
        assertEquals("SERVICE_UNAVAILABLE", v.get("analysis").get("failureReason").asText());
        // the set itself is untouched (NFR-010)
        assertEquals(8, body(as(tokenA, get("/api/sets/" + setId)), 200).get("reps").asInt());

        org.mockito.Mockito.reset(analysis);
        when(analysis.analyze(anyString(), anyString(), anyString())).thenReturn(RESULT);
        body(as(tokenA, post("/api/videos/" + v.get("id").asText() + "/reanalyze")), 202);
        waitForVideo(setId, "COMPLETED");
    }

    @Test
    void validationAndOwnership() throws Exception {
        String setId = newSet();
        body(as(tokenA, multipart("/api/sets/" + setId + "/video")
                .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hi".getBytes()))), 400);
        body(as(tokenA, multipart("/api/sets/" + setId + "/video")
                .file(new MockMultipartFile("file", "empty.mp4", "video/mp4", new byte[0]))), 400);
        body(as(tokenA, multipart("/api/sets/" + setId + "/video")), 400);
        body(as(tokenB, multipart("/api/sets/" + setId + "/video").file(mp4())), 404);
        mvc.perform(multipart("/api/sets/" + setId + "/video").file(mp4())).andExpect(status().isUnauthorized());
    }
}
