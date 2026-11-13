package desktop;

import com.fgol.platform.graphics.fgolCanvas;
import com.fgol.platform.system.Keyb;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL11.*;

/**
 * Solo per il collaudo automatico (proprieta' grab.test=file di script).
 * Righe: "<frame> shot <nome>" | "<frame> click <x> <y>" (coordinate canvas)
 *        "<frame> key <codiceGLFW> <press|release>" | "<frame> quit"
 */
final class TestDriver {
    private final List<String[]> cmds = new ArrayList<>();
    private final File outDir;

    static TestDriver fromProperties() {
        String f = System.getProperty("grab.test");
        if (f == null) return null;
        try { return new TestDriver(new File(f)); } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private TestDriver(File script) throws Exception {
        outDir = new File(System.getProperty("grab.shots", "shots"));
        outDir.mkdirs();
        for (String l : Files.readAllLines(script.toPath())) {
            l = l.trim();
            if (!l.isEmpty() && !l.startsWith("#")) cmds.add(l.split("\\s+"));
        }
    }

    boolean onFrame(long frame, int w, int h) {
        boolean quit = false;
        for (String[] c : cmds) {
            if (Long.parseLong(c[0]) != frame) continue;
            switch (c[1]) {
                case "shot": shot(new File(outDir, c[2] + ".png"), w, h); break;
                case "click":
                    int x = Integer.parseInt(c[2]), y = Integer.parseInt(c[3]);
                    Keyb.onMouseButton(true, x, y);
                    pendingRelease = new int[]{x, y};
                    break;
                case "key": Keyb.onKey(Integer.parseInt(c[2]), c[3].equals("press") ? 1 : 0); break;
                case "quit": quit = true; break;
            }
        }
        if (pendingRelease != null && releaseAt == -1) releaseAt = frame + 3;
        if (pendingRelease != null && frame >= releaseAt) {
            Keyb.onMouseButton(false, pendingRelease[0], pendingRelease[1]);
            pendingRelease = null; releaseAt = -1;
        }
        return quit;
    }
    private int[] pendingRelease; private long releaseAt = -1;

    private static void shot(File f, int w, int h) {
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = ((h - 1 - y) * w + x) * 4;
            img.setRGB(x, y, ((buf.get(i) & 255) << 16) | ((buf.get(i + 1) & 255) << 8) | (buf.get(i + 2) & 255));
        }
        MemoryUtil.memFree(buf);
        try { javax.imageio.ImageIO.write(img, "png", f); } catch (Exception e) { e.printStackTrace(); }
        System.out.println("[test] screenshot " + f.getName() + " canvas=" + fgolCanvas.displayWidth + "x" + fgolCanvas.displayHeight);
    }
}
