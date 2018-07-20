package com.muwire.core.files

import java.util.stream.Collectors

import com.muwire.core.DownloadedFile
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

import groovy.json.JsonSlurper
import net.i2p.data.Base32
import net.i2p.data.Destination

class PersisterService {

	final File location
	final EventBus listener
	final int interval
	final Timer timer
	final def fileSource
	
	PersisterService(File location, EventBus listener, int interval, def fileSource) {
		this.location = location
		this.listener = listener
		this.interval = interval
		this.fileSource = fileSource
		timer = new Timer("file persister", true)
	}
	
	void start() {
		timer.schedule({load()} as TimerTask, 1000)
	}
	
	private void load() {
		if (location.exists() && location.isFile()) {
			def slurper = new JsonSlurper()
			try {
				location.eachLine {
					if (it.trim().length() == 0)
						return
					def parsed = slurper.parseText it
					def event = fromJson parsed
					if (event != null)
						listener.publish event
				}
			} catch (IllegalArgumentException|NumberFormatException e) {
				// abort loading
			}
		}
		timer.schedule({persistFiles()} as TimerTask, 0, interval)
	}
	
	private static FileLoadedEvent fromJson(def json) {
		if (json.file == null || json.length == null || json.infoHash == null || json.hashList == null)
			throw new IllegalArgumentException()
		if (!(json.hashList instanceof List))
			throw new IllegalArgumentException()
			
		def file = new File(json.file)
		file = file.getCanonicalFile()
		if (!file.exists() || file.isDirectory())
			return null
		long length = Long.valueOf(json.length)
		if (length != file.length())
			return null
		
		List hashList = (List) json.hashList
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		hashList.each {
			byte [] hash = Base32.decode it.toString()
			if (hash == null)
				throw new IllegalArgumentException()
			baos.write hash
		}
		byte[] hashListBytes = baos.toByteArray()
		
		InfoHash ih = InfoHash.fromHashList(hashListBytes)
		byte [] root = Base32.decode(json.infoHash.toString())
		if (root == null)
			throw new IllegalArgumentException()
		if (!Arrays.equals(root, ih.getRoot()))
			return null
			
		if (json.sources != null) {
			List sources = (List)json.sources
			Set<Destination> sourceSet = sources.stream().map({d -> new Destination(d.toString())}).collect Collectors.toSet()
			DownloadedFile df = new DownloadedFile(file, ih, sourceSet)
			return new FileLoadedEvent(loadedFile : df)
		}
		 
		SharedFile sf = new SharedFile(file, ih)
		return new FileLoadedEvent(loadedFile: sf)
		
	}
	
	private void persistFiles() {
		
	}
}
