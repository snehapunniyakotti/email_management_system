package com.gmail.demo.service.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

import com.gmail.demo.entity.OAuth2Token;
import com.gmail.demo.repository.OAuth2TokenRepository;

/*
 * ClientAuthorizationRequiredException: [client_authorization_required] Authorization required for Client Registration Id: google
 * `OAuth2AuthorizedClientService` (likely the default in-memory one, or JPA-backed) **does not yet have an entry** for `("google", "system")`.
 * 
 * So when `manager.authorize(request)` runs, Spring tries to look up the saved authorized client for 
 * `(registrationId=google, principal=system)` → nothing found → Spring falls back to **authorization\_code flow** → 
 * but since there’s no web request to perform login/consent, it fails with **`ClientAuthorizationRequiredException`**.
 * 
 * already having the refresh token (e.g. from DB, config, or a first login), you can **construct an `OAuth2AuthorizedClient`
 * manually** and save it in the `OAuth2AuthorizedClientService`.
 * 
 * Spring Security’s OAuth2AuthorizedClientManager to load/update tokens from your DB instead of its default in-memory storage
 * */

@Service
public class JpaOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

	private final OAuth2TokenRepository tokenRepo;
	private final ClientRegistrationRepository clientRegistrationRepository;
	 private final OAuth2AuthorizedClientService inMemoryDelegate;

	public JpaOAuth2AuthorizedClientService(OAuth2TokenRepository tokenRepo,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.tokenRepo = tokenRepo;
		this.clientRegistrationRepository = clientRegistrationRepository;
		 this.inMemoryDelegate = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Override
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
			String principalName) {
		OAuth2Token token = tokenRepo.findByUserEmail(principalName);
		if (token != null) {
			ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);

			OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
					token.getAccessToken(), token.getExpiresAt().minusSeconds(55 * 60), // minutes // when issued
					token.getExpiresAt() // expiry
			);

			OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(token.getRefreshToken(),
					token.getExpiresAt().minusSeconds(30 * 24 * 60 * 60) // days // dummy issuedAt
			);

			@SuppressWarnings("unchecked")
			T client = (T) new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
			return client;
		}
		return inMemoryDelegate.loadAuthorizedClient(clientRegistrationId, principalName);
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {	
		/* keep in-memory updated */
        inMemoryDelegate.saveAuthorizedClient(authorizedClient, principal);
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		tokenRepo.deleteByUserEmail(principalName);
		inMemoryDelegate.removeAuthorizedClient(clientRegistrationId, principalName);
	}

}
