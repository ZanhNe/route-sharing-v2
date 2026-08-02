package com.zanh.route_sharing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InternalPageController {
    @GetMapping("/internal/login")
    String login() {
        return "internal/login";
    }

    @GetMapping("/internal/access-denied")
    String accessDenied() {
        return "internal/access-denied";
    }

    @GetMapping("/internal")
    String index() {
        return "internal/index";
    }
}
