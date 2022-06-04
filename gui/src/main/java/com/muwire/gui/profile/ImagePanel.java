package com.muwire.gui.profile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (image == null)
            return;
        
        Dimension dim = getSize();
        graphics.clearRect(0,0,(int)dim.getWidth(), (int)dim.getHeight());
        graphics.drawImage(image,
                (int)(dim.getWidth() / 2) - (image.getWidth() / 2),
                (int)(dim.getHeight() / 2) - (image.getHeight() / 2),
                null);
    }
}
