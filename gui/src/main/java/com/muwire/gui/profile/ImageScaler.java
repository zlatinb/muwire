package com.muwire.gui.profile;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageScaler {
    
    public static BufferedImage scaleToMax(BufferedImage source) {
        return scale(source, ProfileConstants.MAX_IMAGE_SIZE);
    }
    
    public static BufferedImage scaleToThumbnail(BufferedImage source) {
        return scale(source, ProfileConstants.MAX_THUMBNAIL_SIZE);
    }
    
    private static BufferedImage scale(BufferedImage source, int maxSize) {
        double scale = 1.0;
        int maxDim = Math.max(source.getHeight(), source.getWidth());
        if (maxDim <= maxSize)
            return source;
        scale = maxSize * 1.0 / maxDim;
        
        BufferedImage rv = new BufferedImage((int)(source.getWidth() * scale),
                (int)(source.getHeight() * scale),
                BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        rv = op.filter(source, rv);
        return rv;
    }
}
