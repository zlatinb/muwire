package com.muwire.gui.profile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ThumbnailIcon implements Icon {
    
    BufferedImage image;
    
    public ThumbnailIcon(byte [] rawData) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(rawData));
        image = ImageScaler.scaleToThumbnail(img);
    }
    
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        ((Graphics2D)g).drawImage(image, x, y, null);
    }

    @Override
    public int getIconWidth() {
        return image.getWidth();
    }

    @Override
    public int getIconHeight() {
        return image.getHeight();
    }
}
