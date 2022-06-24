package com.muwire.gui.profile

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileFetchEvent
import com.muwire.core.profile.MWProfileFetchStatus
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.profile.UIProfileFetchEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.gui.HTMLSanitizer
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import griffon.transform.Observable

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class ViewProfileModel {
    @MVCMember @Nonnull
    ViewProfileView view

    Core core
    Persona persona
    UUID uuid
    String profileTitle
    MWProfileHeader profileHeader
    
    @Observable MWProfileFetchStatus status
    MWProfile profile
    
    @Observable TrustLevel trustLevel
    
    private boolean registered
    
    void mvcGroupInit(Map<String, String> args) {
        if (profile != null)
            profileHeader = profile.getHeader()
        if (profileHeader != null)
            profileTitle = HTMLSanitizer.sanitize(profileHeader.getTitle())
        
        setTrustLevel(core.getTrustService().getLevel(persona.getDestination()))
        core.getEventBus().register(TrustEvent.class, this)
    }
    
    boolean fetchEnabled() {
        status != MWProfileFetchStatus.CONNECTING && status != MWProfileFetchStatus.FETCHING
    }
    
    void register() {
        if (registered)
            return
        registered = true
        core.getEventBus().register(MWProfileFetchEvent.class, this)
    }
    
    void fetch() {
        core.getEventBus().publish(new UIProfileFetchEvent(uuid: uuid, host: persona))
    }
    
    void mvcGroupDestroy() {
        core.getEventBus().unregister(TrustEvent.class, this)
        if (registered)
            core.getEventBus().unregister(MWProfileFetchEvent.class, this)
    }
    
    void onMWProfileFetchEvent(MWProfileFetchEvent event) {
        if (uuid != event.uuid)
            return
        runInsideUIAsync {
            status = event.status
            if (status == MWProfileFetchStatus.FINISHED) {
                view.profileFetched(event.profile)
                profile = event.profile
                profileHeader = profile.getHeader()
            }
        }
    }
    
    void onTrustEvent(TrustEvent event) {
        if (event.persona != persona)
            return
        runInsideUIAsync {
            trustLevel = event.level
        }
    }
}
