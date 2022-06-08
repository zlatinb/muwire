package com.muwire.gui.profile

import com.muwire.core.profile.MWProfileHeader

import javax.swing.Icon

/**
 * Implementation of PersonaOrProfile when only a
 * profile header is available.
 */
class ProfileHeaderPOP extends AbstractPOP {
    private final MWProfileHeader header
    private Icon icon
    ProfileHeaderPOP(MWProfileHeader header) {
        this.header = header
    }

    @Override
    Icon getThumbnail() {
        if (icon == null) {
            icon = new ThumbnailIcon(header.getThumbNail())
        }
        return icon
    }

    @Override
    MWProfileHeader getHeader() {
        return header
    }
}
