package com.fgol.platform.graphics;

import android.graphics.Bitmap;
import com.fgol.platform.system.Host;
import desktop.Res;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/** Caricatore di texture: stessa logica dell'originale per i file PVR/RAW, PNG via ImageIO. */
public class ImageLoader {
    public static final boolean pow2TexturesOnly = true;

    private static int nearestPow2(int size) {
        for (int i = 0; i < 16; i++) if ((1 << i) >= size) return 1 << i;
        return 16;
    }

    private static int reverseByteOrder(int val) {
        return ((val >> 24) & 255) | (((val >> 16) & 255) << 8) | (((val >> 8) & 255) << 16) | ((val & 255) << 24);
    }

    public static fgolImage createImage(String name) {
        int dot = name.indexOf(".");
        String basename = dot < 0 ? name : name.substring(0, dot);
        int resID = Res.id(basename);
        if (resID == 0 && (resID = Res.id(basename + Host.GFX_EXTENSION)) == 0) resID = Res.id(basename + "_16b");
        fgolImage img = null;
        try {
            InputStream is = Host.openResourceAsStream(name);
            if (is != null) {
                DataInputStream dis = new DataInputStream(is);
                String type = "";
                for (int i = 0; i < 4; i++) type += (char) dis.readByte();
                if (type.endsWith("PNG")) img = loadPNG(name, resID);
                else if (type.endsWith("RAW")) img = loadRAW(dis, resID);
                else if (type.startsWith("DDS")) img = null;
                else img = loadPVR(dis, resID);
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (img != null) img.filename = name;
        return img;
    }

    public static fgolImage loadPNG(String name, int resID) throws IOException {
        int dot = name.indexOf(".");
        File f = Res.file(dot < 0 ? name : name.substring(0, dot));
        BufferedImage src = ImageIO.read(f);
        int w = src.getWidth(), h = src.getHeight();
        Bitmap bmp = Bitmap.createBitmap(nearestPow2(w), nearestPow2(h), Bitmap.Config.ARGB_8888);
        for (int y = 0; y < h; y++) src.getRGB(0, y, w, 1, bmp.pixels, y * bmp.width, w);
        fgolImage img = new fgolImage(bmp, resID);
        img.origWidth = w;
        img.origHeight = h;
        img.ooWidth = 1.0f / bmp.width;
        img.ooHeight = 1.0f / bmp.height;
        return img;
    }

    public static fgolImage loadPVR(DataInputStream dis, int resID) throws IOException {
        int compressedFormat;
        boolean alpha;
        int pixelFormat;
        String strTextureFormat;
        int w = 512;
        int h = 512;
        int fmt = -1;
        for (int i = 1; i < 13; i++) {
            int val = dis.readInt();
            if (i == 1) {
                w = reverseByteOrder(val);
            }
            if (i == 2) {
                h = reverseByteOrder(val);
            }
            if (i == 4) {
                fmt = reverseByteOrder(val) & 61695;
            }
        }
        switch (fmt) {
            case 19:
                compressedFormat = 32856;
                alpha = false;
                pixelFormat = 33635;
                strTextureFormat = "OGL565";
                break;
            case 32784:
                compressedFormat = 32856;
                alpha = true;
                pixelFormat = 32819;
                strTextureFormat = "OGL4444";
                break;
            case 32785:
                compressedFormat = 32856;
                alpha = true;
                pixelFormat = 32820;
                strTextureFormat = "OGL5551";
                break;
            case 32786:
                compressedFormat = 32856;
                alpha = true;
                pixelFormat = 5121;
                strTextureFormat = "OGL8888";
                break;
            case 32792:
                compressedFormat = 35843;
                alpha = true;
                pixelFormat = -1;
                strTextureFormat = "PVRTC2";
                break;
            case 32793:
                compressedFormat = 35842;
                alpha = true;
                pixelFormat = -1;
                strTextureFormat = "PVRTC4";
                break;
            default:
                compressedFormat = 32856;
                alpha = true;
                pixelFormat = 32820;
                strTextureFormat = "OGL5551";
                break;
        }
        fgolImage img = loadCompressedTexture(dis, resID, w, h, compressedFormat);
        if (img != null) {
            img.alpha = alpha;
            img.pixelFormat = pixelFormat;
            img.strTextureFormat = strTextureFormat;
        }
        return img;
    }

    public static fgolImage loadRAW(DataInputStream dis, int resID) throws IOException {
        dis.readInt();
        int w = dis.readInt();
        int h = dis.readInt();
        fgolImage img = loadCompressedTexture(dis, resID, w, h, 32856);
        img.strTextureFormat = "OGL8888";
        return img;
    }

    public static fgolImage loadCompressedTexture(DataInputStream dis, int resID, int w, int h, int compressedFormat) throws IOException {
        byte[] data = new byte[dis.available()];
        dis.read(data);
        ByteBuffer compressedTexture = ByteBuffer.wrap(data);
        compressedTexture.position(0);
        fgolImage img = new fgolImage(compressedTexture, resID);
        img.compressedFormat = compressedFormat;
        img.origWidth = w;
        img.origHeight = h;
        img.ooWidth = 1.0f / w;
        img.ooHeight = 1.0f / h;
        img.vramFootprint = data.length;
        return img;
    }
}
