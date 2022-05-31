package com.muwire.gui.resultdetails

import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.profile.ResultPOP
import static com.muwire.gui.Translator.trans

import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import java.awt.Component

class ResultListCellRenderer implements ListCellRenderer<ResultPOP>{
    @Override
    Component getListCellRendererComponent(JList<? extends ResultPOP> list,
                                           ResultPOP value, int index,
                                           boolean isSelected, boolean cellHasFocus) {
        JLabel rv = new JLabel()
        String text = "<html>" + PersonaCellRenderer.htmlize(value.getEvent().sender) + "</html>"
        rv.setText(text)
        
        if (value.getThumbnail() != null)
            rv.setIcon(value.getThumbnail())
        else
            rv.setIcon(null)
        
        if (value.getTitle() != null)
            rv.setToolTipText(value.getTitle())
        else
            rv.setToolTipText(trans("NO_PROFILE"))
        
        if (!isSelected) {
            rv.setForeground(list.getForeground())
            rv.setBackground(list.getSelectionBackground())
        } else {
            rv.setForeground(list.getSelectionForeground())
            rv.setBackground(list.getSelectionBackground())
        }
        rv
    }
}
