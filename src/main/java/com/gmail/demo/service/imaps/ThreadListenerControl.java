package com.gmail.demo.service.imaps;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadListenerControl {
	private final AtomicBoolean running = new AtomicBoolean(true);

	boolean isRunning() {
		return running.get();
	}

	void stop() {
		running.set(false);  
	}
}
