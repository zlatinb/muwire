package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.SharedFile

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import net.i2p.data.SigningPrivateKey
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionWizardModel {
    List<SharedFile> files
    SigningPrivateKey spk
    Persona me
    
    long timestamp
    @Observable String root
    @Observable String comment
 
    long totalSize() {
        long rv = 0
        files.each { 
            rv += it.getCachedLength()
        }
        rv
    }   
}