package com.muwire.gui.resultdetails

import com.muwire.core.search.UIResultEvent
import com.muwire.gui.PersonaCellRenderer

import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer
import java.awt.Component

class ResultListCellRenderer implements ListCellRenderer<UIResultEvent>{
    @Override
    Component getListCellRendererComponent(JList<? extends UIResultEvent> list,
                                           UIResultEvent value, int index,
                                           boolean isSelected, boolean cellHasFocus) {
        JLabel rv = new JLabel()
        String text = "<html>" + PersonaCellRenderer.htmlize(value.sender) + "</html>"
        rv.setText(text)
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
