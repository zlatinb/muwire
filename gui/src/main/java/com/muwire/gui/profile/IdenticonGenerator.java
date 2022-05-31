package com.muwire.gui.profile;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.charset.StandardCharsets;

/**
 * Copy-paste from
 * https://gist.github.com/GrenderG/caaae6de29e456438a6f9bd6394ca566
 */
public class IdenticonGenerator {
    
    public static BufferedImage generateIdenticon(String text) {
        int width = 5, height = 5;
        BufferedImage identicon = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = identicon.getRaster();
        
        byte [] hash = text.getBytes(StandardCharsets.UTF_8);
        int[] background = new int[] {255,255,255, 0};
        int[] foreground = new int[] {hash[0] & 255, hash[1] & 255, hash[2] & 255, 255};
        
        for (int x = 0; x < width; x++) {
            int i = ( x < 3) ? x : 4 - x;
            for (int y = 0; y < height; y++) {
                int [] pixelColor;
                if ((hash[i] >> y & 1) == 1)
                    pixelColor = foreground;
                else
                    pixelColor = background;
                raster.setPixel(x, y, pixelColor);
            }
        }
        
        BufferedImage finalImage = new BufferedImage(ProfileConstants.MAX_IMAGE_SIZE, 
                ProfileConstants.MAX_IMAGE_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(ProfileConstants.MAX_IMAGE_SIZE / (width * 1.0),
                ProfileConstants.MAX_IMAGE_SIZE / (height * 1.0));
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        finalImage = op.filter(identicon, finalImage);
        return finalImage;
    }
}
