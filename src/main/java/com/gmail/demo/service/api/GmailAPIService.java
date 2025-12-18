package com.gmail.demo.service.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.gmail.demo.entity.OAuth2Token;
import com.gmail.demo.repository.OAuth2TokenRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

import jakarta.servlet.http.HttpSession;

@Service
public class GmailAPIService {

	@Autowired
	private OAuth2AuthorizedClientManager authorizedClientManager;
	@Autowired
	private OAuth2TokenRepository oauthTokenRepository;
	@Autowired
	private RSAService rsaService;

	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String CLIENT_ID ;
	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String CLIENT_SECRET ;
//	@Value("${spring.security.oauth2.client.provider.google.token-uri}")
	private final String TOKEN_URL = "https://oauth2.googleapis.com/token";

	private final RestTemplate restTemplate = new RestTemplate();
	private static final Logger log = LoggerFactory.getLogger(GmailAPIService.class);

	
	/* this method used to get the access token from security context holder */
	public OAuth2AccessToken getValidAccessToken() {

		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication != null && authentication.isAuthenticated()) {
				log.info("authentication.toString() : " + authentication.toString());
				Object principal = authentication.getPrincipal();
				if (principal instanceof UserDetails) {
					log.info("((UserDetails) principal).getUsername()  ::::: "
							+ ((UserDetails) principal).getUsername());
				} else {
					log.info("  principal.toString() ::::: " + principal.toString());
				}
				OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("google")
						.principal(authentication)
//					.principal("snehademo27@gmail.com")
						.build();

				OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);

				if (authorizedClient == null) {
					throw new IllegalStateException("Authorization failed!");
				}

				return authorizedClient.getAccessToken();
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("exception occured in getValid AccessToken  :: e.message "+ e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public Gmail getGmailService(String accessToken, Instant expiresAt) throws GeneralSecurityException, IOException {

		AccessToken token = new AccessToken(accessToken, Date.from(expiresAt));

		GoogleCredentials credentials = GoogleCredentials.create(token)
				.createScoped(Collections.singletonList("https://mail.google.com/"));

		HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

		return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
				requestInitializer).setApplicationName("GmailCloneApp").build();
	}

	public Gmail getGmailService(String accessToken, String refreshToken, Instant expiresAt)
			throws GeneralSecurityException, IOException {

		log.error("accessToken : " + accessToken);
		log.error("refreshToken : " + refreshToken);
		log.error("expiresAt : " + expiresAt);
		log.error("CLIENT_ID : " + CLIENT_ID);
		log.error("CLIENT_SECRET : " + CLIENT_SECRET);
		log.error("TOKEN_URL : " + TOKEN_URL);

		UserCredentials userCredentials = UserCredentials.newBuilder().setClientId(CLIENT_ID)
				.setClientSecret(CLIENT_SECRET).setAccessToken(new AccessToken(accessToken, Date.from(expiresAt)))
				.setRefreshToken(refreshToken).build();

		Gmail gmailService = new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(),
				GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(userCredentials))
				.setApplicationName("GmailCloneApp").build();

		return gmailService;
	}

	/* getting snoozed message ids by calling gmail api  */
	public List<String> getSnoozedMsgIds() throws GeneralSecurityException, IOException  {

		OAuth2AccessToken token = getValidAccessToken();

		if (token != null) {
			log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!");
			log.info(token.getTokenValue());
			log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!");

			OAuth2Token oauth2Token = oauthTokenRepository.findByAccessToken(token.getTokenValue());

			log.info("oauth2Token.isTokenExpires() :: " + oauth2Token.isTokenExpires());
			log.info(" Instant.now() :: "+Instant.now());
			
			/* check is existing access token is expired */
			if (oauth2Token.isTokenExpires()) {
				oauth2Token = refreshAccessTokenIfExpired(oauth2Token);
			}

			Gmail service = getGmailService(oauth2Token.getAccessToken(), oauth2Token.getRefreshToken(),
					oauth2Token.getExpiresAt());

			ListMessagesResponse response = service.users().messages().list("me").setQ("in:snoozed").execute();

			List<String> msgIds = new ArrayList<String>();
			List<Message> messages = response.getMessages();
			if (messages != null && !messages.isEmpty()) {
				for (Message msg : messages) {

					String GmailMsgId = msg.getId();

					Message fullMessage = service.users().messages().get("me", GmailMsgId).setFormat("full").execute();
					List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();
					for (MessagePartHeader header : headers) {
						if ("Message-ID".equalsIgnoreCase(header.getName())) {
							msgIds.add(header.getValue());
						}
					}
				}
			}
			return msgIds;
		}
		return new ArrayList<String>();

	}

	/* refresh  (expired)access token using refresh token by calling oauth2 auth end point */
	public OAuth2Token refreshAccessTokenIfExpired(OAuth2Token token) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException  {

		MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("grant_type", "refresh_token");
		requestBody.add("refresh_token", rsaService.decryptData(token.getRefreshToken()));
		requestBody.add("client_id", CLIENT_ID);
		requestBody.add("client_secret", CLIENT_SECRET);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, requestEntity, Map.class);

		if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
			Map<String, Object> body = response.getBody();
 
			String newAccessToken = (String) body.get("access_token");
			Number expiresInNumber = (Number) body.get("expires_in"); // safe for Integer/Long
			long expiresIn = expiresInNumber.longValue();

			token.setAccessToken(newAccessToken);
			token.setExpiresAt(Instant.now().plusSeconds(expiresIn));

			if (body.containsKey("refresh_token")) {
				token.setRefreshToken((String) body.get("refresh_token"));
			}

			log.info("Access token refreshed successfully for email", token.getUserEmail());
			return oauthTokenRepository.save(token);

		} else {
			log.error("Failed to refresh token for email : statuscode", token.getUserEmail(), response.getStatusCode());
			return token;
		}

	}

}
