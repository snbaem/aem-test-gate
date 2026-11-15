package com.example.demo.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class HeroModelTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(HeroModel.class);
    }

    private HeroModel adapt(String path, Object... props) {
        Resource resource = context.create().resource(path, props);
        HeroModel model = resource.adaptTo(HeroModel.class);
        assertNotNull(model);
        return model;
    }

    @Test
    void usesTrimmedTitleWhenPresent() {
        HeroModel model = adapt("/content/hero", "title", "  Welcome  ", "subtitle", "Hi");
        assertEquals("Welcome", model.getDisplayTitle());
        assertEquals("Hi", model.getSubtitle());
        assertFalse(model.isEmpty());
    }

    @Test
    void fallsBackToUntitledWhenTitleBlank() {
        HeroModel model = adapt("/content/hero", "title", "   ");
        assertEquals("Untitled", model.getDisplayTitle());
    }

    @Test
    void isEmptyWhenNoProperties() {
        HeroModel model = adapt("/content/hero");
        assertTrue(model.isEmpty());
        assertEquals("Untitled", model.getDisplayTitle());
        assertNull(model.getSubtitle());
    }

    @Test
    void notEmptyWhenOnlySubtitleSet() {
        HeroModel model = adapt("/content/hero", "subtitle", "Sub only");
        assertFalse(model.isEmpty());
    }
}
