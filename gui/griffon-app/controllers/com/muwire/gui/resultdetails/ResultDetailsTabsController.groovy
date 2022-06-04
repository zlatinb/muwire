package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.gui.CopyPasteSupport
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ViewProfileHelper
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class ResultDetailsTabsController {
 
    @MVCMember @Nonnull
    ResultDetailsTabsView view
    @MVCMember @Nonnull
    ResultDetailsTabsModel model
    
    @ControllerAction
    void browse() {
        Persona p = view.selectedSender()?.getPersona()
        if (p == null)
            return
        String groupId = UUID.randomUUID().toString()
        Map<String,Object> params = new HashMap<>()
        params['host'] = p
        params['core'] = model.core

        mvcGroup.parentGroup.createMVCGroup("browse", groupId, params)
    }
    
    @ControllerAction
    void viewProfile() {
        PersonaOrProfile pop = view.selectedSender()
        if (pop == null)
            return

        ViewProfileHelper.initViewProfileGroup(model.core, mvcGroup, pop)
    }
}
