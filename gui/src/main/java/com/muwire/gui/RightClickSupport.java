package com.muwire.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class RightClickSupport {

    /**
     * Updates the selection of the component that was right-clicked on.
     * 
     * If the component is a table and it is in single-selection mode, the row that 
     * was right-clicked on gets selected.  If it is in multi-selection mode, the row
     * gets selected only if nothing else is currently selected.
     * 
     * If the component is a tree, then only if nothing is selected does the current path
     * get selected.
     * @param event
     * @return false if no right-click menu should be shown
     */
    public static boolean processRightClick(MouseEvent event) {
        Component component = event.getComponent();
        if (component instanceof JTable) {
            JTable table = (JTable) component;
            Point point = event.getPoint();
            ListSelectionModel selectionModel = table.getSelectionModel();
            if (selectionModel.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION) {
                int row = table.rowAtPoint(point);
                if (row < 0)
                    return false;
                selectionModel.setSelectionInterval(row, row);
            } else {
                int [] rows = table.getSelectedRows();
                if (rows.length > 0)
                    return true;
                int row = table.rowAtPoint(point);
                if (row < 0)
                    return false;
                selectionModel.setSelectionInterval(row, row);
            }
        } else if (component instanceof JTree) {
            JTree tree = (JTree) component;
            if (tree.getSelectionPaths() != null)
                return true;
            int row = tree.getRowForLocation(event.getX(), event.getY());
            if (row < 0)
                return false;
            tree.setSelectionRow(row);
        }
        return true;
    }
}
