package com.example.demo.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.demo.services.GreetingService;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class GreetingServiceImplTest {

    private final AemContext context = new AemContext();

    private GreetingService register(Map<String, Object> config) {
        context.registerInjectActivateService(new GreetingServiceImpl(), config);
        return context.getService(GreetingService.class);
    }

    @Test
    void greetsByNameWithDefaultPrefix() {
        assertEquals("Hello, Asha!", register(Map.of()).greet("Asha"));
    }

    @Test
    void trimsName() {
        assertEquals("Hello, Asha!", register(Map.of()).greet("  Asha "));
    }

    @Test
    void usesDefaultNameWhenBlankOrNull() {
        GreetingService service = register(Map.of());
        assertEquals("Hello, World!", service.greet(null));
        assertEquals("Hello, World!", service.greet(" "));
    }

    @Test
    void honoursOverriddenConfig() {
        GreetingService service = register(Map.of("prefix", "Namaste", "defaultName", "Friend"));
        assertEquals("Namaste, Friend!", service.greet(""));
        assertEquals("Namaste, Ravi!", service.greet("Ravi"));
    }
}
