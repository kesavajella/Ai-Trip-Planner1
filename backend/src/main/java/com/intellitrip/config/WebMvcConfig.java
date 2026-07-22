package com.intellitrip.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/spa/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/spa/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/spa/images/");

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/spa/")
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected org.springframework.core.io.Resource resolveResourceInternal(
                            jakarta.servlet.http.HttpServletRequest request,
                            String requestPath,
                            java.util.List<? extends org.springframework.core.io.Resource> locations,
                            org.springframework.web.servlet.resource.ResourceResolverChain chain) {
                        if (requestPath.startsWith("/api/")) {
                            return null;
                        }
                        org.springframework.core.io.Resource resource =
                                super.resolveResourceInternal(request, requestPath, locations, chain);
                        if (resource == null) {
                            return new ClassPathResource("/spa/index.html");
                        }
                        return resource;
                    }
                });
    }
}
