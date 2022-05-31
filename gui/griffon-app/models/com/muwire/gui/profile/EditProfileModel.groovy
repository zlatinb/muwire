package com.muwire.gui.profile

import com.muwire.core.Core
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class EditProfileModel {
    @MVCMember @Nonnull
    EditProfileView view
    @MVCMember @Nonnull
    EditProfileController controller

    Core core
}
