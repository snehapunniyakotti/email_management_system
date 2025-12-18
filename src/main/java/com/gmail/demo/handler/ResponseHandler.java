package com.gmail.demo.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHandler {
	public static ResponseEntity<Object> responseWithObject(Object responseBody, HttpStatus status) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("data", responseBody);
		map.put("status", status.value());
		return new ResponseEntity<Object>(map, status);
	}

	public static ResponseEntity<Object> responseWithString(String message, HttpStatus status) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("msg", message);
		map.put("status", status.value());
		return new ResponseEntity<Object>(map, status);
	}
	public static ResponseEntity<Object> responseWithDraftMessageId(String messageId, HttpStatus status) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("msgId", messageId);
		map.put("status", status.value());
		return new ResponseEntity<Object>(map, status);
	}
	
	public static ResponseEntity<Object> responseWithObject(Object responseBody,String errMsg, HttpStatus status) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("data", responseBody);
		map.put("errMsg", errMsg);
		map.put("status", status.value());
		return new ResponseEntity<Object>(map, status);
	}

}
