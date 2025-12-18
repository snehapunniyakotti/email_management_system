package com.gmail.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gmail.demo.entity.ScheduledEmail;
import com.gmail.demo.util.MailUtil;

public interface ScheduledEmailRepo extends JpaRepository<ScheduledEmail, Long>{

	List<ScheduledEmail> findByStatusAndScheduledTimeBefore(MailUtil.Status status, LocalDateTime scheduledTime);
}
