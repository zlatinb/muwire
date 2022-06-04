package com.muwire.gui.profile

import com.muwire.core.Core
import griffon.core.mvc.MVCGroup

class ViewProfileHelper {
    
    static void initViewProfileGroup(Core core, MVCGroup group, PersonaOrProfile pop) {
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params.core = core
        params.persona = pop.getPersona()
        params.uuid = uuid
        params.profileTitle = pop.getTitle()
        params.profileHeader = pop.getHeader()
        
        group.createMVCGroup("view-profile", uuid.toString(), params)
    }
}
