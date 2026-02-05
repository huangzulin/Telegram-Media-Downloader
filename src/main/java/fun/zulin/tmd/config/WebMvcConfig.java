package fun.zulin.tmd.config;

import fun.zulin.tmd.utils.DownloadDirectoryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private DownloadDirectoryManager directoryManager;
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
        try {
            String downloadPath = directoryManager.getDownloadPath().toUri().toString();
            String videosPath = directoryManager.getVideosPath().toUri().toString();
            String thumbnailsPath = directoryManager.getThumbnailsPath().toUri().toString();
            String tempPath = directoryManager.getTempPath().toUri().toString();
            
            // 映射/downloads根目录
            registry.addResourceHandler("/downloads/**")
                    .addResourceLocations(downloadPath)
                    .setCachePeriod(3600);
                    
            // 专门映射/downloads/videos子目录（优先级更高）
            registry.addResourceHandler("/downloads/videos/**")
                    .addResourceLocations(videosPath)
                    .setCachePeriod(3600);
                    
            // 映射/downloads/thumbnails子目录（视频封面）
            registry.addResourceHandler("/downloads/thumbnails/**")
                    .addResourceLocations(thumbnailsPath)
                    .setCachePeriod(3600);
                    
            // 映射/downloads/temp子目录
            registry.addResourceHandler("/downloads/temp/**")
                    .addResourceLocations(tempPath)
                    .setCachePeriod(3600);
        } catch (Exception e) {
            // 如果目录管理器不可用，回退到默认路径
            registry.addResourceHandler("/downloads/**")
                    .addResourceLocations("file:./downloads/")
                    .setCachePeriod(3600);
            registry.addResourceHandler("/downloads/videos/**")
                    .addResourceLocations("file:./downloads/videos/")
                    .setCachePeriod(3600);
            registry.addResourceHandler("/downloads/thumbnails/**")
                    .addResourceLocations("file:./downloads/thumbnails/")
                    .setCachePeriod(3600);
            registry.addResourceHandler("/downloads/temp/**")
                    .addResourceLocations("file:./downloads/temp/")
                    .setCachePeriod(3600);
        }
                
        // 映射/data路径到实际的data目录
        registry.addResourceHandler("/data/**")
                .addResourceLocations("file:./data/")
                .setCachePeriod(3600);
    }
}
