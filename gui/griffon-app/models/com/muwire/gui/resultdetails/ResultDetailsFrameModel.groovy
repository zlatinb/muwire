package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ResultDetailsFrameModel {
    
    Core core
    UIResultEvent resultEvent
    Set<Persona> senders
}
