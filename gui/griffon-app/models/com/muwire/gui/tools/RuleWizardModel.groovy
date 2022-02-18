package com.muwire.gui.tools

import com.muwire.core.Core
import com.muwire.core.content.MatchAction
import griffon.core.artifact.GriffonModel
import griffon.metadata.ArtifactProviderFor
import griffon.transform.Observable

@ArtifactProviderFor(GriffonModel)
class RuleWizardModel {
    Core core
    
    @Observable boolean regex
    @Observable MatchAction action = MatchAction.RECORD
}
