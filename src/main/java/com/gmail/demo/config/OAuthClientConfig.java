package com.gmail.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.gmail.demo.service.api.JpaOAuth2AuthorizedClientService;

@Configuration
public class OAuthClientConfig {


	/*
	 * This creates a **`AuthorizedClientServiceOAuth2AuthorizedClientManager`**.
	 * 
	 * This one is built for **background / service-layer use**. It doesn’t need an
	 * active servlet request. Instead, it just looks up stored authorized clients
	 * (including refresh tokens) from the **`jpaAuthorizedClientService(custom)`** and
	 * can refresh tokens offline.
	 * 
	 */
	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			JpaOAuth2AuthorizedClientService jpaAuthorizedClientService) {

		AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, jpaAuthorizedClientService);

		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.authorizationCode().refreshToken().build();

		manager.setAuthorizedClientProvider(authorizedClientProvider);

		return manager;
	}
}
