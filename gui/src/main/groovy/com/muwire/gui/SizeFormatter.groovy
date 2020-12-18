package com.muwire.gui

import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format

class SizeFormatter {

    private static final DecimalFormat fmt = new DecimalFormat("##0.##")

    static void format(long value, StringBuffer sb) {
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
