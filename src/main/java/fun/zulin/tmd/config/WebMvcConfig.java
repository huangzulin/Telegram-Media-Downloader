package fun.zulin.tmd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射/downloads根目录
        registry.addResourceHandler("/downloads/**")
                .addResourceLocations("file:./downloads/")
                .setCachePeriod(3600);
                
        // 专门映射/downloads/videos子目录（优先级更高）
        registry.addResourceHandler("/downloads/videos/**")
                .addResourceLocations("file:./downloads/videos/")
                .setCachePeriod(3600);
                
        // 映射/downloads/temp子目录
        registry.addResourceHandler("/downloads/temp/**")
                .addResourceLocations("file:./downloads/temp/")
                .setCachePeriod(3600);
                
        // 映射/data路径到实际的data目录
        registry.addResourceHandler("/data/**")
                .addResourceLocations("file:./data/")
                .setCachePeriod(3600);
    }
}
