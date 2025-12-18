package com.gmail.demo.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gmail.demo.entity.EmailMetadata;
import java.util.Date;

public interface EmailRepo extends JpaRepository<EmailMetadata, Long> {
//	  Page<EmailMetadata> findByFolder(String folder, Pageable pageable);
	boolean existsByFolderAndUid(String folder, long uid);

	@Query("""
			select count(e) > 0 from EmailMetadata e
			where e.folder like :folder% and
			e.uid = :uid
			""")
	boolean existsByFolderStartWithAndUid(String folder, long uid);

	@Query("""
			select e from EmailMetadata e
			where e.folder like :folder% AND
			e.deleteFlag = false
			""")
	List<EmailMetadata> findByStartsWithFolderName(String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder AND
			e.deleteFlag = false
			""")
	List<EmailMetadata> findByFolder(String folder);

	@Query("select distinct(e.folder) from EmailMetadata e")
	List<String> fetchAllFolderNames();

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder AND
			e.deleteFlag = false
			""")
	Page<EmailMetadata> findByFolder(@Param("folder") String folder, Pageable pageable);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder AND
			e.deleteFlag = false AND
			e.snoozed = false
			""")
	Page<EmailMetadata> findByFolderAndSnoozed(String folder, Pageable pageable);

	Page<EmailMetadata> findBySnoozed(boolean snoozed, Pageable pageable);

	Page<EmailMetadata> findBySnoozedAndFolder(boolean snoozed, String folder, Pageable pageable);

	@Query("""
			select e from EmailMetadata e
			where folder in ('INBOX', 'INBOX/personal') AND
			e.inboxUnique = true  AND
			e.deleteFlag = false AND
			e.snoozed = false
			   """)
	Page<EmailMetadata> fetchCustomInboxMessage(Pageable pageable);

	@Query("""
			select count(e) from EmailMetadata e
			where e.folder = :folder
			""")
	long countByFolder(@Param("folder") String folder);

	List<EmailMetadata> findByUid(long uid);

	List<EmailMetadata> findByFolderAndUid(String folder, long uid);

	@Query("""
			select e from EmailMetadata e
			where folder like :folder% and
			uid = :uid
			""")
	List<EmailMetadata> findByFolderStartWithAndUid(String folder, long uid);

	@Query("""
			select e from EmailMetadata e
			where folder like :folder% and
			e.deleteFlag = false
			""")
	Page<EmailMetadata> findByFolderStartWith(String folder, Pageable pageable);

	List<EmailMetadata> findByMessageId(String messageId);

	List<EmailMetadata> findByMessageIdAndFolder(String messageId, String folder);

	@Query("""
			select e.uid from EmailMetadata e
			where folder like :folder%
			""")
	List<Long> findUidsByFolder(String folder);

	List<EmailMetadata> findBySnoozed(boolean snoozed);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			(LOWER(e.fromAddr) like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.toAddr) like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.cc) like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.bcc) like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.subject) like LOWER(CONCAT('%',:word,'%')) or
			e.body like %:word% )
			""")
	Set<EmailMetadata> searchEmailByWord(String word, String folder);

	@Query("""
			select e from EmailMetadata e
			where LOWER(e.fromAddr) like LOWER(CONCAT('%',:fromAddr,'%')) and
			e.folder = :folder
			""")
	Set<EmailMetadata> findByfromAddrAndFolder(String fromAddr, String folder);

	@Query("""
			select e from EmailMetadata e
			where LOWER(e.toAddr) like LOWER(CONCAT('%',:toAddr,'%')) and
			e.folder = :folder
			""")
	Set<EmailMetadata> findByToAddrAndFolder(String toAddr, String folder);

	@Query("""
			   select e from EmailMetadata e
			   where LOWER(e.subject) like LOWER(CONCAT('%',:subject,'%')) and
			   e.folder = :folder
			""")
	Set<EmailMetadata> findBySubjectAndFolder(String subject, String folder);

	@Query("""
			select e from EmailMetadata e 
			where e.folder = :folder and
			(LOWER(e.fromAddr) not like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.toAddr) not like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.cc) not like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.bcc) not like LOWER(CONCAT('%',:word,'%')) or
			LOWER(e.subject) not like LOWER(CONCAT('%',:word,'%')) or
			e.body not like %:word% )
			""")
	Set<EmailMetadata> searchEmailByNotWord(String word, String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			sentAt between :beforedate and :afterdate
			""")
	Set<EmailMetadata> findBySentAtAndFolder(Date beforedate, Date afterdate, String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			sizeBytes < :sizeBytes
			""")
	Set<EmailMetadata> findBySizeBytesLesserAndFolder(long sizeBytes, String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			sizeBytes > :sizeBytes
			""")
	Set<EmailMetadata> findBySizeBytesGreaterAndFolder(long sizeBytes, String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			read = :read
			""")
	Set<EmailMetadata> findByReadAndFolder(boolean read, String folder);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder and
			e.hasAttachments = :hasAttachments
			""")
	Set<EmailMetadata> findByFolderAndHasAttachments(String folder, boolean hasAttachments);

	@Query("""
			select e from EmailMetadata e
			where e.folder = :folder AND
			e.deleteFlag = false
			""")
	Set<EmailMetadata> findByFolderName(String folder);

}
