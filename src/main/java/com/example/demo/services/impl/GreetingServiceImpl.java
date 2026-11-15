package com.example.demo.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.example.demo.services.GreetingService;

@Component(service = GreetingService.class, immediate = true)
@Designate(ocd = GreetingServiceImpl.Config.class)
public class GreetingServiceImpl implements GreetingService {

    @ObjectClassDefinition(name = "Demo Greeting Service")
    public @interface Config {
        @AttributeDefinition(name = "Greeting prefix")
        String prefix() default "Hello";

        @AttributeDefinition(name = "Default name")
        String defaultName() default "World";
    }

    private volatile String prefix;
    private volatile String defaultName;

    @Activate
    @Modified
    protected void configure(Config config) {
        this.prefix = config.prefix();
        this.defaultName = config.defaultName();
    }

    @Override
    public String greet(String name) {
        String who = StringUtils.isBlank(name) ? defaultName : name.trim();
        return prefix + ", " + who + "!";
    }
}
