package com.muwire.gui.chat

import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.SizeFormatter
import com.muwire.gui.UISettings
import com.muwire.core.mulinks.FileMuLink

import static com.muwire.gui.Translator.trans

class FileLinkLabel extends MuLinkLabel {
    
    FileLinkLabel(FileMuLink link, UISettings settings, boolean border) {
        super(link, settings, border)
    }
    
    protected String getVisibleText() {
        FileMuLink link = (FileMuLink) this.link
        
        StringBuffer sb = new StringBuffer()
        SizeFormatter.format(link.fileSize, sb)
        
        link.name + " (" + sb.toString() + trans("BYTES_SHORT") + ")"
    }
}
