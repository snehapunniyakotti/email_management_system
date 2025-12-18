package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.OAuth2Token;
import java.util.List;


public interface OAuth2TokenRepository extends JpaRepository<OAuth2Token, Long> {
    OAuth2Token findByUserEmail(String email);
    
    OAuth2Token findByAccessToken(String accessToken);
    
    void deleteByUserEmail(String userEmail);
}
