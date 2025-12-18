package com.gmail.demo.config;

import java.util.Arrays;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gmail.demo.handler.OAuth2LoginSuccessHandler;
import com.gmail.demo.service.api.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Autowired
	private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsServiceImpl);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManage(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.authenticationProvider(authenticationProvider());
		return authenticationManagerBuilder.build();
	}


	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://127.0.0.1:5501/", 
				"http://10.20.20.157:8080/", "http://localhost:8081/" , "http://127.0.0.1:5500/")); // here
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // Apply this configuration to all paths
		return source;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/h2-console/**", "/auth/**", "/user/**", "/emails/**",
								"/imap/**","/file/**","/verify/**","/csrf/**")
						.permitAll().anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/auth/**", "/user/**","/emails/**",
						 "/imap/**","/file/**","/verify/**","/csrf/**").csrfTokenRepository(csrfTokeRepo()))
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
				.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
				.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
//				.formLogin(Customizer.withDefaults())
				.build(); 
	}
//	"/emails/**",
	
	@Bean
	public CsrfTokenRepository csrfTokeRepo() {
		HttpSessionCsrfTokenRepository repository  = new HttpSessionCsrfTokenRepository();
		repository.setSessionAttributeName("csrf");
		return repository; 
	}

	@Bean
	public AuthenticationSuccessHandler successHandler() {
		return (request, response, authentication) -> {
			// After successful login, redirect to /home
			System.err.println("printing inside the successHandler : request :: " + request + " : response :: "
					+ response + " : authentication :: " + authentication);
			
			response.sendRedirect("/home");
		};
	}
	
	  @Bean
	   public JavaMailSender javaMailSender() {
	       JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	       mailSender.setHost("smtp.gmail.com");
	       mailSender.setPort(587);
	       mailSender.setUsername("snehademo27@gmail.com");
	       mailSender.setPassword("zejgtdmdlhqresbm");

	       Properties props = mailSender.getJavaMailProperties();
	       props.put("mail.smtp.auth", "true");
	       props.put("mail.smtp.starttls.enable", "true");

	       return mailSender;
	   }
	
	
}
