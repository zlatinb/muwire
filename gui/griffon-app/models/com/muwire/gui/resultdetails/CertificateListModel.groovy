package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.profile.ResultPOP
import griffon.core.artifact.GriffonModel
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class CertificateListModel {
    
    @MVCMember @Nonnull
    CertificateListView view
    
    Core core
    List<ResultPOP> results
    String uuid
    
    Map<Persona, MVCGroup> tabGroups = new HashMap<>()
    
    void mvcGroupInit(Map<String,String> args) {
        for(ResultPOP resultPOP : results) {
            tabGroups.put(resultPOP.getEvent().sender, createTabGroup(resultPOP.getEvent()))
        }
    }
    
    void mvcGroupDestroy() {
        tabGroups.values().each {it.destroy()}
    }
    
    void addResult(ResultPOP resultPOP) {
        UIResultEvent event = resultPOP.getEvent()
        if (event.certificates == 0)
            return
        if (tabGroups.containsKey(event.sender))
            return
        tabGroups.put(event.sender, createTabGroup(event))
        results << resultPOP
        view.refresh()
    }
    
    private MVCGroup createTabGroup(UIResultEvent event) {
        String mvcId = "certs_" + uuid + "_" + event.sender.toBase64() + "_" + Base64.encode(event.infohash.getRoot())
        def params = [:]
        params.core = core
        params.resultEvent = event
        params.uuid = uuid
        mvcGroup.createMVCGroup("certificate-tab", mvcId, params)
    }
}
