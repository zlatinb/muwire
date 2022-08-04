package com.muwire.gui.chat

import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.SizeFormatter
import com.muwire.gui.UISettings
import com.muwire.gui.mulinks.CollectionMuLink
import static com.muwire.gui.Translator.trans

class CollectionLinkLabel extends MuLinkLabel {
    CollectionLinkLabel(CollectionMuLink link, UISettings settings, boolean border) {
        super(link, settings, border)
    }

    @Override
    protected String getVisibleText() {
        CollectionMuLink link = (CollectionMuLink) this.link

        StringBuffer sb = new StringBuffer()
        SizeFormatter.format(link.totalSize, sb)
        
        return HTMLSanitizer.escape(link.name) + " (" + link.numFiles + " " + trans("FILES") + 
                " " + sb.toString() + trans("BYTES_SHORT") + ")"
    }
}
