package com.intellitrip.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrip.model.User;
import com.intellitrip.service.AuthService;
import com.intellitrip.service.FirebaseAuthService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final FirebaseAuthService firebaseAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(AuthService authService, FirebaseAuthService firebaseAuthService) {
        this.authService = authService;
        this.firebaseAuthService = firebaseAuthService;
    }

@GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        model.addAttribute("firebaseAvailable", firebaseAuthService.isAvailable());
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/dashboard";
        model.addAttribute("firebaseAvailable", firebaseAuthService.isAvailable());
        model.addAttribute("countries", loadCountries());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String name,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam(required = false) String country,
                         @RequestParam(required = false) String countryCode,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = authService.signUp(name, email, password, country, countryCode);
            authService.setSession(session, user);
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Signup error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred. Please try again.");
            return "redirect:/signup";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                         @RequestParam String password,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = authService.signIn(email, password);
            authService.setSession(session, user);
            if (user.getCountry() == null || user.getCountry().isBlank()) {
                return "redirect:/select-country";
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Login error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred. Please try again.");
            return "redirect:/login";
        }
    }

    @PostMapping("/auth/firebase")
    public String firebaseAuth(@RequestParam String idToken,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            var decodedToken = firebaseAuthService.verifyIdToken(idToken);
            if (decodedToken == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid authentication token.");
                return "redirect:/login";
            }
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            if (name == null || name.isBlank()) {
                name = decodedToken.getUid();
            }
            User user = authService.signInWithGoogle(email, name);
            authService.setSession(session, user);
            if (user.getCountry() == null || user.getCountry().isBlank()) {
                return "redirect:/select-country";
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Firebase auth error", e);
            redirectAttributes.addFlashAttribute("error", "Authentication failed: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/select-country")
    public String selectCountryPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) return "redirect:/login";
        model.addAttribute("countries", loadCountries());
        return "select-country";
    }

    @PostMapping("/select-country")
    public String selectCountry(@RequestParam String country,
                                @RequestParam String countryCode,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        try {
            authService.updateCountry(userId, country, countryCode);
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Update country error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Failed to update country.");
            return "redirect:/select-country";
        }
    }

    private List<Map<String, Object>> loadCountries() {
        try (InputStream in = new ClassPathResource("static/data/countries.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to load countries", e);
            return List.of();
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(HttpSession session, Model model) {
        model.addAttribute("firebaseAvailable", firebaseAuthService.isAvailable());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendPasswordReset(@RequestParam String email,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (!firebaseAuthService.isAvailable()) {
                redirectAttributes.addFlashAttribute("error", "Password reset is not available. Please contact support.");
                return "redirect:/forgot-password";
            }
            String resetLink = firebaseAuthService.generatePasswordResetLink(email);
            if (resetLink != null) {
                redirectAttributes.addFlashAttribute("success", "Password reset email sent! Check your inbox.");
            } else {
                redirectAttributes.addFlashAttribute("error", "No account found with that email address.");
            }
        } catch (Exception e) {
            log.error("Password reset error", e);
            redirectAttributes.addFlashAttribute("error", "Failed to send reset email: " + e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
