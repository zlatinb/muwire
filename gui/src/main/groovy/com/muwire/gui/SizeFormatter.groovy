package com.muwire.gui

import java.text.DecimalFormat

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

        fmt.format(val,sb, java.text.DontCareFieldPosition.INSTANCE)
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
}
