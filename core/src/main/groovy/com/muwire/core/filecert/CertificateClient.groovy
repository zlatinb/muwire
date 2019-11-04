package com.muwire.core.filecert

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level

import net.i2p.data.Base64

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InvalidSignatureException
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log

@Log
class CertificateClient {
    private final EventBus eventBus
    private final I2PConnector connector
    
    private final ExecutorService fetcherThread = Executors.newSingleThreadExecutor()
    
    CertificateClient(EventBus eventBus, I2PConnector connector) {
        this.eventBus = eventBus
        this.connector = connector
    }
    
    void onUIFetchCertificatesEvent(UIFetchCertificatesEvent e) {
        fetcherThread.execute({
            Endpoint endpoint = null
            try {
                eventBus.publish(new CertificateFetchEvent(status : CertificateFetchStatus.CONNECTING))
                endpoint = connector.connect(e.host.destination)
                
                String infoHashString = Base64.encode(e.infoHash.getRoot())
                OutputStream os = endpoint.getOutputStream()
                os.write("CERTIFICATES ${infoHashString}\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                
                InputStream is = endpoint.getInputStream()
                String code = DataUtil.readTillRN(is)
                if (!code.startsWith("200"))
                    throw new IOException("invalid code $code")

                // parse all headers
                Map<String,String> headers = new HashMap<>()
                String header
                while((header = DataUtil.readTillRN(is)) != "" && headers.size() < Constants.MAX_HEADERS) {
                    int colon = header.indexOf(':')
                    if (colon == -1 || colon == header.length() - 1)
                        throw new IOException("invalid header $header")
                    String key = header.substring(0, colon)
                    String value = header.substring(colon + 1)
                    headers[key] = value.trim()
                }

                if (!headers.containsKey("Count"))
                    throw new IOException("No count header")
                
                int count = Integer.parseInt(headers['Count'])
                
                // start pulling the certs
                eventBus.publish(new CertificateFetchEvent(status : CertificateFetchStatus.FETCHING, count : count))
                
                DataInputStream dis = new DataInputStream(is)
                for (int i = 0; i < count; i++) {
                    int size = dis.readUnsignedShort()
                    byte [] tmp = new byte[size]
                    dis.readFully(tmp)
                    Certificate cert = null
                    try {
                        cert = new Certificate(new ByteArrayInputStream(tmp))
                    } catch (IOException | InvalidSignatureException ignore) {
                        continue
                    }
                    if (cert.infoHash == e.infoHash)
                        eventBus.publish(new CertificateFetchedEvent(certificate : cert))
                }
            } catch (Exception bad) {
                log.log(Level.WARNING,"Fetching certificates failed", bad)
                eventBus.publish(new CertificateFetchEvent(status : CertificateFetchStatus.FAILED))
            } finally {
                endpoint?.close()
            }
        })
    }
}
