package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import com.muwire.gui.profile.PersonaOrProfile
import griffon.core.artifact.GriffonModel
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ResultDetailsFrameModel {
    
    Core core
    UIResultEvent resultEvent
    Collection<PersonaOrProfile> senders
    String uuid
}
