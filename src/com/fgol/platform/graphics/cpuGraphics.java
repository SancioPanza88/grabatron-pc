package com.fgol.platform.graphics;

/**
 * Disegno su immagini in memoria. Sull'originale questi percorsi (screenshot per le
 * transizioni, copia di fotogrammi) non producevano nulla con le texture PVR:
 * qui sono innocui e non generano eccezioni.
 */
public class cpuGraphics extends fgolGraphics {
    public static cpuGraphics instance;
    private final fgolImage target;
    public cpuGraphics() { this(null); }
    public cpuGraphics(fgolImage target) { this.target = target; instance = this; }
    @Override public void drawImage(fgolImage img, int x, int y) {
        if (target == null || target.bitmap == null || img == null || img.bitmap == null) return;
        android.graphics.Bitmap s = img.bitmap, d = target.bitmap;
        for (int j = 0; j < s.height; j++) {
            int dy = y + j; if (dy < 0 || dy >= d.height) continue;
            for (int i = 0; i < s.width; i++) {
                int dx = x + i; if (dx < 0 || dx >= d.width) continue;
                int p = s.pixels[j * s.width + i];
                if ((p >>> 24) != 0) d.pixels[dy * d.width + dx] = p;
            }
        }
        target.invalidate();
    }
}
