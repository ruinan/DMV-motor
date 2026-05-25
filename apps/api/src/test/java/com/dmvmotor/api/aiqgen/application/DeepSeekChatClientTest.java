package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekChatClientTest {

    private MockWebServer server;
    private DeepSeekChatClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        RestClient rc = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
        client = new DeepSeekChatClient(rc, "deepseek-chat", 1000);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void chat_happyPath_returnsContent() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "choices": [{"message": {"content": "hello back"}}],
                          "usage": {"prompt_tokens": 10, "completion_tokens": 5}
                        }
                        """));

        String result = client.chat("sys", "hi");
        assertThat(result).isEqualTo("hello back");
    }

    @Test
    void chat_serverErrors_throwsAiQGenException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("internal error"));

        assertThatThrownBy(() -> client.chat("sys", "hi"))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("DeepSeek request failed");
    }

    @Test
    void chat_missingChoices_throwsAiQGenException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"usage\": {}}"));

        assertThatThrownBy(() -> client.chat("sys", "hi"))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("missing choices");
    }

    @Test
    void chat_emptyChoices_throwsAiQGenException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\": []}"));

        assertThatThrownBy(() -> client.chat("sys", "hi"))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("missing choices");
    }

    @Test
    void chat_choiceMissingMessage_throwsAiQGenException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"finish_reason\":\"stop\"}]}"));

        assertThatThrownBy(() -> client.chat("sys", "hi"))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("missing content");
    }

    @Test
    void chat_blankContent_throwsAiQGenException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"\"}}]}"));

        assertThatThrownBy(() -> client.chat("sys", "hi"))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("missing content");
    }
}
