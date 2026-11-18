package com.example.demo.models;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/** Sling Model backing a teaser component with a title, description and link. */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TeaserModel {

    private static final String CONTENT_ROOT = "/content/";
    private static final String HTML_EXTENSION = ".html";

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String linkUrl;

    @PostConstruct
    protected void init() {
        title = StringUtils.trimToNull(title);
        description = StringUtils.trimToNull(description);
        linkUrl = externalize(StringUtils.trimToNull(linkUrl));
    }

    /**
     * Appends {@code .html} to internal {@code /content/...} page paths that have no extension,
     * keeping any query string or fragment. External and already-extended links are untouched.
     */
    private static String externalize(String link) {
        if (link == null || !link.startsWith(CONTENT_ROOT)) {
            return link;
        }
        int suffixStart = StringUtils.indexOfAny(link, '?', '#');
        String path = suffixStart < 0 ? link : link.substring(0, suffixStart);
        String suffix = suffixStart < 0 ? "" : link.substring(suffixStart);

        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        if (lastSegment.isEmpty() || lastSegment.contains(".")) {
            return link;
        }
        return path + HTML_EXTENSION + suffix;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    /** A teaser is renderable only when it has both a title and a link. */
    public boolean isConfigured() {
        return title != null && linkUrl != null;
    }
}
