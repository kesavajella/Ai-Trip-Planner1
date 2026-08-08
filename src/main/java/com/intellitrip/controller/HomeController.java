package com.intellitrip.controller;

import com.intellitrip.model.User;
import com.intellitrip.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        model.addAttribute("user", user);
        return "index";
    }
}

