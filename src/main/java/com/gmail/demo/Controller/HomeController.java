package com.gmail.demo.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {
	@GetMapping("/home")
	public String homePage(HttpSession session) {
		if ((boolean) session.getAttribute("oauth2")) {
			return "home"; // Thymeleaf template: home.html
		}

		return "redirect:/login";
	}

	@GetMapping("/login")
	public String login() {
		return "redirect:/login"; // maps to login.html
	}
}
