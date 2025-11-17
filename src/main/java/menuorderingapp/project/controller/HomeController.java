package menuorderingapp.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for convenient URL redirects
 * Makes URLs more user-friendly and easier to remember
 */
@Controller
public class HomeController {

    // Root - Goes to customer menu (main landing page)
    @GetMapping("/")
    public String home() {
        return "redirect:/customer/menu";
    }

    // Convenient customer URL
    @GetMapping("/customer")
    public String customerHome() {
        return "redirect:/customer/menu";
    }

    // Convenient cashier URL - Goes to dashboard
    @GetMapping("/cashier")
    public String cashierHome() {
        return "redirect:/cashier/dashboard";
    }

    // Convenient login URL - /login instead of /auth/login
    @GetMapping("/login")
    public String login() {
        return "redirect:/auth/login";
    }
}
