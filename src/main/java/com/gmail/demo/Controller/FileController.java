package com.gmail.demo.Controller;

import java.io.IOException;
import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gmail.demo.service.api.FileService;

@RestController
@RequestMapping("/file")
public class FileController {

	@Autowired
	private FileService fileService;
	
	private static final Logger log = LoggerFactory.getLogger(FileController.class);
	private static final Logger knownExpLog = LoggerFactory.getLogger("known-exception-log");
	private static final Logger unKnownExpLog = LoggerFactory.getLogger("unknown-exception-log");

	@GetMapping("/download/{id}")
	public ResponseEntity<Resource> getfileById(@PathVariable Integer id) {
		log.info("getfileById (+)");
		log.info("getfileById param - id : "+id);
		try {
			System.out.println(" id in file controller  "+id);
			ResponseEntity<Resource> res = fileService.getFileById(id);
			System.out.println(" res in file download : "+res);
			return res;
		} catch (MalformedURLException e) {
			knownExpLog.error("CFCGFBI-001" + e.getMessage());
			e.printStackTrace(); 
			return ResponseEntity.internalServerError().headers(header -> header.add("error", "CFCGFBI-001")).build();
		}
		catch (IOException e) {
			knownExpLog.error("CFCGFBI-002" + e.getMessage());
			e.printStackTrace(); 
			return ResponseEntity.internalServerError().headers(header -> header.add("error", "CFCGFBI-002")).build(); 
		}
		catch (Exception e) {   
			unKnownExpLog.error("CFCGFBI-003" + e.getMessage());   
			e.printStackTrace(); 
			return ResponseEntity.internalServerError().headers(header -> header.add("error", "CFCGFBI-003")).build();
		}finally {
			log.info("getfileById (-)");
		}
	}  

}
