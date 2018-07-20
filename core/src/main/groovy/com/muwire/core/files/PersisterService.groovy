package com.muwire.core.files

class PersisterService {

	final File location
	final def listener
	final int interval
	final Timer timer
	
	PersisterService(File location, def listener, int interval) {
		this.location = location
		this.listener = listener
		this.interval = interval
		timer = new Timer("file persister", true)
	}
	
	void start() {
		timer.schedule({load()} as TimerTask, 1000)
	}
	
	private void load() {
		// TODO: load shared files from location
		timer.schedule({processEvents()} as TimerTask, 0, interval)
	}
	
	private void processEvents() {
		
	}
}
