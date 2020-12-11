package com.muwire.gui

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format

import static com.muwire.gui.Translator.trans

class SizeRenderer extends DefaultTableCellRenderer {
    private final String bShort;
    private final DecimalFormat fmt = new DecimalFormat("##0.##")
    private final StringBuffer sb = new StringBuffer(32)
    SizeRenderer() {
        setHorizontalAlignment(JLabel.CENTER)
        bShort = trans("BYTES_SHORT")
    }
    @Override
    JComponent getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            // this is very strange, but it happens.  Probably a swing bug?
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        }
        Long l = (Long) value
        format(l)
        sb.append(bShort)
        setText(sb.toString())
        if (isSelected) {
            setForeground(table.getSelectionForeground())
            setBackground(table.getSelectionBackground())
        } else {
            setForeground(table.getForeground())
            setBackground(table.getBackground())
        }
        this
    }

    private void format(long value) {
        sb.setLength(0)
        if (value < 1024) {
            sb.append(value).append(' ')
            return
        }

        double val = value
        int scale = 0
        while(val >= 1024) {
            scale++
            val /= 1024d
        }
        if (val >= 200)
            fmt.setMaximumFractionDigits(0)
        else if (val >= 20)
            fmt.setMaximumFractionDigits(1)
        fmt.format(val,sb, DontCareFieldPosition.INSTANCE)
        sb.append(' ')
        switch(scale) {
            case 1 : sb.append('K'); break;
            case 2 : sb.append('M'); break;
            case 3 : sb.append('G'); break;
            case 4 : sb.append('T'); break;
            case 5 : sb.append('P'); break;
            case 6 : sb.append('E'); break;
            case 7 : sb.append('Z'); break;
            case 8 : sb.append('Y'); break;
            default :
                sb.append(' ')
        }
    }

    private static class DontCareFieldPosition extends FieldPosition {
        // The singleton of DontCareFieldPosition.
        static final FieldPosition INSTANCE = new java.text.DontCareFieldPosition();

        private final Format.FieldDelegate noDelegate = new Format.FieldDelegate() {
            public void formatted(Format.Field attr, Object value, int start,
                                  int end, StringBuffer buffer) {
            }
            public void formatted(int fieldID, Format.Field attr, Object value,
                                  int start, int end, StringBuffer buffer) {
            }
        };

        private DontCareFieldPosition() {
            super(0);
        }

        Format.FieldDelegate getFieldDelegate() {
            return noDelegate;
        }
    }
}
