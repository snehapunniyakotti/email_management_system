package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.EmailMessage;

public interface EmailRepository extends JpaRepository<EmailMessage, Long> {

}
