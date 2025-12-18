package com.gmail.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.MailTemplete;


public interface MailTempleteRepo extends JpaRepository<MailTemplete, Integer> {



}
