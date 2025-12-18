package com.gmail.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gmail.demo.entity.User;
import com.gmail.demo.repository.UserRepository;
import com.gmail.demo.service.security.Google2FAService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class Google2FAController {

	   @Autowired
	    private Google2FAService google2faService;

	    @Autowired
	    private UserRepository userRepository;

	    @GetMapping("/setup-2fa")
	    public String showSetupPage(HttpSession session, Model model) {
	    	System.out.println("(boolean) session.getAttribute(\"oauth2\") :: "+ session.getAttribute("oauth2"));
	    	if ((boolean) session.getAttribute("oauth2")) {
	    		model.addAttribute("qrUri", session.getAttribute("qrUri"));
	 	        return "setup-2fa";
			}
	    	return "redirect:/login";
	    }
 
	    @GetMapping("/verify-2fa")
	    public String showVerifyPage(HttpSession session) {
	    	System.out.println("(boolean) session.getAttribute(\"oauth2\") :: "+ session.getAttribute("oauth2"));
	    	if ((boolean) session.getAttribute("oauth2")) {
	    		 return "2fa-verification";
			}
	    	return "redirect:/login";
	       
	    }

	    @PostMapping("/verify")
	    public String verifyCode(@RequestParam String code, HttpSession session) {
	    	System.out.println(" request code : "+code);
	        String email = (String) session.getAttribute("email");
	        
	        if (email == null) {
	            return "redirect:/login";
	        }
	        
	        String secret = (String) session.getAttribute("secret");
	        User user = userRepository.findByEmail(email);
	        
	        if (user == null) {
	            return "redirect:/login";
	        }
	        
	        System.out.println(" session email "+ email);
	        System.out.println(" session  email : "+ email + " session user details : "+ user +" user.getSecretKey() " +user.getSecretKey());
	        
	        System.out.println("secret in session ::: "+secret);
	      
	        boolean isvalid = google2faService.verifyCode(user.getSecretKey(), code);
	        if(isvalid) {
	        	return "redirect:/home";  
	        }
	        return "redirect:/verify-2fa?error=true";
	    }
	    
	    
	    @GetMapping("/reset-qr")
	    public String resetTwoFactorAuth(HttpSession session, Model model) {
	        String email = (String) session.getAttribute("email");

	        if (email == null) {
	            return "redirect:/login";
	        }

	        System.out.println(" email in reset-2fa ::: "+email);
	        // Generate new secret and QR code
	        String newSecret = google2faService.generateSecretKey();
	        session.setAttribute("secret", newSecret);

	        String qrUri = "";
			try {
				qrUri = google2faService.getQrCodeUrl(email, newSecret);
			} catch (Exception e) {
				e.printStackTrace();
			}
	        session.setAttribute("qrUri", qrUri);
	        model.addAttribute("qrUri", qrUri);

	        // Persist the new secret in the DB
	        User user = userRepository.findByEmail(email);
	        if (user != null) {
	            user.setSecretKey(newSecret);
	            userRepository.save(user);
	        }
	        
	        if (user == null) {
	            return "redirect:/login";
	        }
	        
	        System.out.println(" User inside the reset-2fa :::: "+user);

	        return "setup-2fa";
	    }
	    
	    @GetMapping("/remove-qr")
	    public String removeQR(HttpSession session, Model model) {
	        String email = (String) session.getAttribute("email");

	        if (email == null) {
	            return "redirect:/login";
	        }

	        System.out.println(" email in remove-qr ::: "+email);
	  
	        session.setAttribute("secret", "");
	        session.setAttribute("qrUri", "");

	        // Persist the empty secret in the DB 
	        User user = userRepository.findByEmail(email);
	        if (user != null) {
	            user.setSecretKey("");
	            userRepository.save(user);
	        }
	
	        System.out.println(" User inside the remove-qr :::: "+user);

	        return "redirect:/login";
	    }
	    
	    @GetMapping("/remove-2fa")
	    public String removeTwoFactorAuth(HttpSession session, Model model) {
	        String email = (String) session.getAttribute("email");

	        if (email == null) {
	            return "redirect:/login";
	        }

	        System.out.println(" email in remove-2fa ::: "+email);
	  
	        session.setAttribute("secret", "");
	        session.setAttribute("qrUri", "");

	        // Persist the empty secret and disable 2fa in the DB 
	        User user = userRepository.findByEmail(email);
	        if (user != null) {
	            user.setSecretKey("");
	            user.setTwoFactorEnabled(false);
	            userRepository.save(user);
	        }
	        
	        System.out.println(" User inside the remove-2fa :::: "+user);

	        return "redirect:/home";
	    }
	    	    
	   
}
