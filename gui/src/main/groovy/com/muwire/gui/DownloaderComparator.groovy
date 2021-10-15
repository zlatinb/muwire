package com.muwire.gui

import com.muwire.core.download.Downloader

class DownloaderComparator implements Comparator<Downloader>{

    @Override
    public int compare(Downloader o1, Downloader o2) {
        double d1 = o1.donePieces().toDouble() / o1.getNPieces()
        double d2 = o2.donePieces().toDouble() / o2.getNPieces()
        return Double.compare(d1, d2);
    }
}
