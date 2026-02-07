package fun.zulin.tmd.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    
    @GetMapping("/style-test")
    public String styleTest() {
        return "style-test";
    }
}