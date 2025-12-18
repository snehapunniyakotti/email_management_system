package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.EmailBody;

public interface EmailBodyRepo extends JpaRepository<EmailBody, Long> {
}
