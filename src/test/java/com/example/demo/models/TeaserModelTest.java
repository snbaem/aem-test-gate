package com.example.demo.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(AemContextExtension.class)
class TeaserModelTest {

    private static final String PATH = "/content/demo/teaser";
    private final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(TeaserModel.class);
    }

    private TeaserModel adapt(Map<String, Object> props) {
        context.create().resource(PATH, props);
        return context.resourceResolver().getResource(PATH).adaptTo(TeaserModel.class);
    }

    @Test
    void trimsValuesAndIsConfiguredWithTitleAndLink() {
        TeaserModel m = adapt(Map.of("title", "  Hello  ", "description", " Desc ", "linkUrl", " /content/site/en "));
        assertEquals("Hello", m.getTitle());
        assertEquals("Desc", m.getDescription());
        assertEquals("/content/site/en.html", m.getLinkUrl());
        assertTrue(m.isConfigured());
    }

    @Test
    void emptyResourceIsNotConfigured() {
        TeaserModel m = adapt(Map.of());
        assertNull(m.getTitle());
        assertNull(m.getDescription());
        assertNull(m.getLinkUrl());
        assertFalse(m.isConfigured());
    }

    @Test
    void blankValuesBecomeNull() {
        TeaserModel m = adapt(Map.of("title", "   ", "description", "", "linkUrl", " "));
        assertNull(m.getTitle());
        assertNull(m.getDescription());
        assertNull(m.getLinkUrl());
        assertFalse(m.isConfigured());
    }

    @Test
    void titleWithoutLinkIsNotConfigured() {
        assertFalse(adapt(Map.of("title", "Hello")).isConfigured());
    }

    @Test
    void linkWithoutTitleIsNotConfigured() {
        assertFalse(adapt(Map.of("linkUrl", "/content/site/en")).isConfigured());
    }

    @ParameterizedTest
    @CsvSource({
        "/content/site/en,                 /content/site/en.html",
        "/content/site/en?q=1,             /content/site/en.html?q=1",
        "/content/site/en#top,             /content/site/en.html#top",
        "/content/site/en.html,            /content/site/en.html",
        "/content/dam/site/file.pdf,       /content/dam/site/file.pdf",
        "/content/site/,                   /content/site/",
        "https://example.com/page,         https://example.com/page",
        "/apps/site/page,                  /apps/site/page",
        "/contentious/page,                /contentious/page"
    })
    void linkHandling(String input, String expected) {
        assertEquals(expected, adapt(Map.of("title", "T", "linkUrl", input)).getLinkUrl());
    }
}
