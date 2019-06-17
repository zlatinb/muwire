package com.muwire.core.files

import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.stream.Collectors

import com.muwire.core.DownloadedFile
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Service
import com.muwire.core.SharedFile
import com.muwire.core.UILoadedEvent
import com.muwire.core.util.DataUtil

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination

@Log
class PersisterService extends Service {

	final File location
	final EventBus listener
	final int interval
	final Timer timer
	final FileManager fileManager
	
	PersisterService(File location, EventBus listener, int interval, FileManager fileManager) {
		this.location = location
		this.listener = listener
		this.interval = interval
		this.fileManager = fileManager
		timer = new Timer("file persister", true)
	}
	
	void stop() {
		timer.cancel()
	}

    void onUILoadedEvent(UILoadedEvent e) {
        timer.schedule({load()} as TimerTask, 1)
    }
    	
	void load() {
		if (location.exists() && location.isFile()) {
			def slurper = new JsonSlurper()
			try {
				location.eachLine {
					if (it.trim().length() > 0) {
						def parsed = slurper.parseText it
						def event = fromJson parsed
						if (event != null) {
                            log.fine("loaded file $event.loadedFile.file")
							listener.publish event
						}
					}
				}
                listener.publish(new AllFilesLoadedEvent())
			} catch (IllegalArgumentException|NumberFormatException e) {
                log.log(Level.WARNING, "couldn't load files",e)
			}
		} else {
            listener.publish(new AllFilesLoadedEvent())
        }
		timer.schedule({persistFiles()} as TimerTask, 0, interval)
		loaded = true
	}
	
	private static FileLoadedEvent fromJson(def json) {
		if (json.file == null || json.length == null || json.infoHash == null || json.hashList == null)
			throw new IllegalArgumentException()
		if (!(json.hashList instanceof List))
			throw new IllegalArgumentException()
			
		def file = new File(DataUtil.readi18nString(Base64.decode(json.file)))
		file = file.getCanonicalFile()
		if (!file.exists() || file.isDirectory())
			return null
		long length = Long.valueOf(json.length)
		if (length != file.length())
			return null
		
		List hashList = (List) json.hashList
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		hashList.each {
			byte [] hash = Base64.decode it.toString()
			if (hash == null)
				throw new IllegalArgumentException()
			baos.write hash
		}
		byte[] hashListBytes = baos.toByteArray()
		
		InfoHash ih = InfoHash.fromHashList(hashListBytes)
		byte [] root = Base64.decode(json.infoHash.toString())
		if (root == null)
			throw new IllegalArgumentException()
		if (!Arrays.equals(root, ih.getRoot()))
			return null
			
        int pieceSize = 0
        if (json.pieceSize != null)
            pieceSize = json.pieceSize
            
        if (json.sources != null) {
			List sources = (List)json.sources
			Set<Destination> sourceSet = sources.stream().map({d -> new Destination(d.toString())}).collect Collectors.toSet()
			DownloadedFile df = new DownloadedFile(file, ih, pieceSize, sourceSet)
			return new FileLoadedEvent(loadedFile : df)
		}
        
		 
		SharedFile sf = new SharedFile(file, ih, pieceSize)
		return new FileLoadedEvent(loadedFile: sf)
		
	}
	
	private void persistFiles() {
		def sharedFiles = fileManager.getSharedFiles()
        
        File tmp = File.createTempFile("muwire-files", "tmp")
        tmp.deleteOnExit()
		tmp.withPrintWriter { writer ->
			sharedFiles.each { k, v ->
				def json = toJson(k,v)
				json = JsonOutput.toJson(json)
				writer.println json
			}
		}
        Files.copy(tmp.toPath(), location.toPath(), StandardCopyOption.REPLACE_EXISTING)
        tmp.delete()
	}
	
	private def toJson(File f, SharedFile sf) {
		def json = [:]
		json.file = Base64.encode DataUtil.encodei18nString(f.getCanonicalFile().toString())
		json.length = f.length()
		InfoHash ih = sf.getInfoHash()
		json.infoHash = Base64.encode ih.getRoot()
        json.pieceSize = sf.getPieceSize()
		byte [] tmp = new byte [32]
		json.hashList = []
		for (int i = 0;i < ih.getHashList().length / 32; i++) {
			System.arraycopy(ih.getHashList(), i * 32, tmp, 0, 32)
			json.hashList.add Base64.encode(tmp)
		}
		
		if (sf instanceof DownloadedFile) {
			json.sources = sf.sources.stream().map( {d -> d.toBase64()}).collect(Collectors.toList())
		}
		
		json
	}
}
