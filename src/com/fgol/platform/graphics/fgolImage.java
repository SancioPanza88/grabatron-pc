package com.fgol.platform.graphics;

import android.graphics.Bitmap;
import com.fgol.platform.display.SimpleGLRenderer;
import java.io.InputStream;
import java.nio.Buffer;

/** Immagine/texture del gioco (stessa interfaccia della versione Android). */
public class fgolImage {
    public static int dynamicImageCount = 0;
    public Bitmap bitmap;
    public int compressedFormat;
    public Buffer compressedTexture;
    public String filename;
    public float ooHeight;
    public float ooWidth;
    public int origHeight;
    public int origWidth;
    public int resourceID;
    public String strTextureFormat;
    public int vramFootprint;
    public boolean isDynamic = false;
    public boolean invalidated = false;
    public boolean pointSampling = false;
    public int pixelFormat = -1;
    public boolean alpha = false;

    public static fgolImage createImage(int width, int height) {
        fgolImage img = new fgolImage(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), 0);
        img.origWidth = width;
        img.origHeight = height;
        img.resourceID = 511;
        img.isDynamic = true;
        return img;
    }
    public static fgolImage createImage(fgolImage source) { return new fgolImage(source.bitmap, source.resourceID); }
    public static fgolImage createImage(byte[] d, int o, int l) { return createImage(16, 16); }
    public static fgolImage createImage(InputStream stream) { return createImage(16, 16); }
    public static fgolImage createImage(String name) {
        fgolImage img = ImageLoader.createImage(name);
        if (img != null) SimpleGLRenderer.markImageForUpload(img);
        return img;
    }
    public static fgolImage createRGBImage(int[] rgb, int width, int height, boolean processAlpha) { return createImage(16, 16); }
    public static fgolImage createImage(fgolImage bitmap, int x, int y, int width, int height, int transform) { return createImage(16, 16); }

    public fgolImage(Bitmap bitmap, int resourceID) { this.bitmap = bitmap; this.resourceID = resourceID; }
    public fgolImage(Buffer buffer, int resourceID) { this.compressedTexture = buffer; this.resourceID = resourceID; }

    public void reload() {
        fgolImage fresh = ImageLoader.createImage(this.filename);
        if (fresh != null) { this.compressedTexture = fresh.compressedTexture; this.bitmap = fresh.bitmap; }
    }
    public void deleteFromGraphicsMemory() { SimpleGLRenderer.markImageForDeletion(this); }
    public void invalidate() { this.invalidated = true; }
    public cpuGraphics getGraphics() { return new cpuGraphics(this); }
    public int getWidth() { return origWidth; }
    public int getHeight() { return origHeight; }
    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        if (bitmap != null) bitmap.getPixels(rgbData, offset, scanlength, x, y, width, height);
    }
    public boolean isMutable() { return bitmap != null; }
}
