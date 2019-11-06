package com.muwire.clilanterna

import com.muwire.core.filecert.Certificate

class CertificateWrapper {
    private final Certificate certificate
    CertificateWrapper(Certificate certificate) {
        this.certificate = certificate
    }
    
    public String toString() {
        certificate.issuer.getHumanReadableName()
    }
}
