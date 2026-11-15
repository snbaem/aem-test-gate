package com.example.demo.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.demo.services.GreetingService;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({ AemContextExtension.class, MockitoExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class GreetingServletTest {

    private final AemContext context = new AemContext();

    @Mock
    private GreetingService greetingService;

    private GreetingServlet servlet;

    @BeforeEach
    void setUp() {
        context.registerService(GreetingService.class, greetingService);
        servlet = context.registerInjectActivateService(new GreetingServlet());
    }

    @Test
    void returnsJsonWithGreetingForName() throws IOException {
        when(greetingService.greet("Asha")).thenReturn("Hello, Asha!");
        context.request().setParameterMap(Map.of("name", "Asha"));

        servlet.doGet(context.request(), context.response());

        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("UTF-8", context.response().getCharacterEncoding());
        assertEquals("{\"message\":\"Hello, Asha!\"}", context.response().getOutputAsString());
    }

    @Test
    void passesNullWhenNameParameterMissing() throws IOException {
        when(greetingService.greet(null)).thenReturn("Hello, World!");

        servlet.doGet(context.request(), context.response());

        assertEquals("{\"message\":\"Hello, World!\"}", context.response().getOutputAsString());
    }

    @Test
    void escapesQuotesInMessage() throws IOException {
        when(greetingService.greet("Q")).thenReturn("Say \"hi\"");
        context.request().setParameterMap(Map.of("name", "Q"));

        servlet.doGet(context.request(), context.response());

        assertEquals("{\"message\":\"Say \\\"hi\\\"\"}", context.response().getOutputAsString());
    }
}
