package com.gmail.demo.service.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.gmail.demo.entity.InitialFileDetails;
import com.gmail.demo.repository.InitialFileDetailsRepo;

@Service
public class FileService {
	
	@Autowired
	private InitialFileDetailsRepo fileRepository;

	
	public ResponseEntity<Resource> getFileById(Integer id) throws MalformedURLException , IOException {  
			InitialFileDetails f = fileRepository.findById(id)
					.orElseThrow();

			System.out.println(" InitialFileDetails  "+f);
			
			/* path */
			Path fileStorageLocation = Paths.get(f.getLocation());
			Path filePath = fileStorageLocation.resolve(f.getName()+f.getExtention()).normalize();
			
			/* resource */
			Resource resource = new UrlResource(filePath.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.notFound().build();
			} 
			
			/* content type */
			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
	        
			/* header */
	        HttpHeaders headers = new HttpHeaders();
	        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getOgname() + f.getExtention() + "\"");
	        headers.add("Access-Control-Expose-Headers", "Content-Disposition");
	        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

	        return ResponseEntity.ok()
	                .headers(headers)
	                .body(resource);
	}
}
