package com.example.demo.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/** Sling Model backing a simple hero component. */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class HeroModel {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String subtitle;

    private String displayTitle;

    @PostConstruct
    protected void init() {
        displayTitle = StringUtils.isBlank(title) ? "Untitled" : title.trim();
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(title) && StringUtils.isBlank(subtitle);
    }
}
