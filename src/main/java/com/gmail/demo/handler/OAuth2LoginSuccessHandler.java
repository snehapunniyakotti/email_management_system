package com.gmail.demo.handler;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gmail.demo.entity.OAuth2Token;
import com.gmail.demo.entity.User;
import com.gmail.demo.repository.OAuth2TokenRepository;
import com.gmail.demo.repository.UserRepository;
import com.gmail.demo.service.api.GmailAPIService;
import com.gmail.demo.service.api.RSAService;
import com.gmail.demo.service.security.Google2FAService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
	private OAuth2AuthorizedClientService clientService;

	@Autowired
	private OAuth2TokenRepository oAuth2TokenRepository;

	@Autowired
	private RSAService rsaService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Google2FAService google2FAService;
	
	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,Authentication authentication) throws IOException, ServletException {

		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
		String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
		OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(clientRegistrationId,
				authentication.getName());

		String accessToken = client.getAccessToken().getTokenValue();
		String refreshToken = client.getRefreshToken().getTokenValue();
		Instant tokenExpiresAt = client.getAccessToken().getExpiresAt();

		log.info("refreshToken  ::::: " + refreshToken);
		log.info("accessToken  ::::: " + accessToken);
		log.info("client.toString()  ::::: " + client.toString());

		/* DefaultOAuth2User gives you the logged-in user details */
		DefaultOAuth2User oauthUser = (DefaultOAuth2User) oauthToken.getPrincipal();
		/* For Google, email is under the "email" attribute */
		String email = oauthUser.getAttribute("email");
		
		request.getSession().setAttribute("email", email);
		request.getSession().setAttribute("oauth2", true);

		log.info("User's Gmail ID: " + email);

		/* Save tokens securely (encrypted) */
		OAuth2Token entity = oAuth2TokenRepository.findByUserEmail(email);
		if(entity == null) {
			entity = new OAuth2Token();
		}
		try {
			entity.setAccessToken(accessToken);
			if (refreshToken != null) {
				entity.setRefreshToken(rsaService.encryptData(refreshToken));
			}
			entity.setExpiresAt(tokenExpiresAt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		entity.setUserEmail(email);
		oAuth2TokenRepository.save(entity);

		User user = userRepository.findByEmail(email);
		if (user == null) {
			user = new User(oauthUser.getName(), email, "", "USER");
			userRepository.save(user);
		}

		/* Handle 2FA */
		if ((user.getSecretKey() == null || user.getSecretKey().isEmpty()) && user.isTwoFactorEnabled()) {
			String secret = google2FAService.generateSecretKey();
			user.setSecretKey(secret);
			userRepository.save(user);
		
			String qrUri = "";
			try {
				qrUri = google2FAService.getQrCodeUrl(email, secret);
			} catch (Exception e) {
				e.printStackTrace();
			}
			request.getSession().setAttribute("qrUri", qrUri);
			response.sendRedirect("/setup-2fa");
			return;
		}
		
		if(user.isTwoFactorEnabled()) {			
			response.sendRedirect("/verify-2fa");
		}else {	
	        response.sendRedirect("/home");  
		}


	}
}
