package android.graphics;
/** Bitmap minimale in memoria (ARGB, righe dall'alto). */
public class Bitmap {
    public enum Config { ARGB_8888, ARGB_4444, RGB_565, ALPHA_8 }
    public final int width, height;
    public final int[] pixels;
    private boolean recycled;
    private Bitmap(int w, int h) { width = w; height = h; pixels = new int[w * h]; }
    public static Bitmap createBitmap(int w, int h, Config c) { return new Bitmap(w, h); }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isRecycled() { return recycled; }
    public void recycle() { recycled = true; }
    public boolean isMutable() { return true; }
    public boolean hasAlpha() { return true; }
    public void getPixels(int[] out, int offset, int stride, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++)
            System.arraycopy(pixels, (y + j) * width + x, out, offset + j * stride, w);
    }
}
