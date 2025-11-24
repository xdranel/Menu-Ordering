package menuorderingapp.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/customer/menu";
    }

    @GetMapping("/customer")
    public String customerHome() {
        return "redirect:/customer/menu";
    }

    @GetMapping("/cashier")
    public String cashierHome() {
        return "redirect:/cashier/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/auth/login";
    }
}
