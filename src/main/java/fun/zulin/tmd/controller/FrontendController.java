package fun.zulin.tmd.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端页面控制器
 */
@Slf4j
@Controller
public class FrontendController {
    
    /**
     * 主页面
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    /**
     * 健康检查页面重定向
     */
    @GetMapping("/health")
    public String health() {
        return "redirect:/actuator/health";
    }
    
    /**
     * 已下载页面
     */
    @GetMapping("/videos")
    public String downloaded() {
        return "video-gallery";
    }
    
    /**
     * 已下载页面（别名路由）
     */
    @GetMapping("/downloaded")
    public String downloadedAlias() {
        log.info("访问已下载页面（别名路由）");
        return "video-gallery";
    }
}