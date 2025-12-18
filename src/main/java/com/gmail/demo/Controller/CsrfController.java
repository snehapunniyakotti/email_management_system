package com.gmail.demo.Controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csrf")
public class CsrfController {

	@GetMapping("/token")
	public CsrfToken getCsrfToken(CsrfToken token) {
		return token;    
	}
	
	
	/*
	 * Another way of getting csrfToken from the http servlet request
	 * (_csrf) default request name for csrf token in http session 
	 * 
	 * @GetMapping("/csrf") public CsrfToken getCsrfToken(HttpServletRequest
	 * request) { CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
	 * 
	 * return token; }
	 */
}
