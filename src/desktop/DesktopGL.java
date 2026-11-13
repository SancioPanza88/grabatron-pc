package desktop;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL11.*;

/**
 * Implementa l'interfaccia OpenGL ES 1.x usata dal renderer originale
 * sopra OpenGL desktop (profilo di compatibilita'), tramite LWJGL.
 */
public final class DesktopGL implements GL10, GL11, GL11Ext {
    private static final HashMap<Integer, int[]> texSize = new HashMap<>();
    private static final HashMap<Integer, int[]> texCrop = new HashMap<>();
    private static int boundTex = 0;
    private int vpW = 1, vpH = 1;
    public static boolean debug = Boolean.getBoolean("grab.debug");

    public static void noteTexSize(int w, int h) {
        texSize.put(boundTex, new int[]{w, h});
    }

    private static long addr(Buffer b) {
        if (!b.isDirect()) throw new IllegalArgumentException("buffer non diretto");
        if (b instanceof FloatBuffer) return MemoryUtil.memAddress((FloatBuffer) b);
        if (b instanceof ByteBuffer) return MemoryUtil.memAddress((ByteBuffer) b);
        if (b instanceof ShortBuffer) return MemoryUtil.memAddress((ShortBuffer) b);
        if (b instanceof IntBuffer) return MemoryUtil.memAddress((IntBuffer) b);
        throw new IllegalArgumentException("tipo di buffer non gestito " + b.getClass());
    }

    @Override public void glVertexPointer(int size, int type, int stride, Buffer p) { nglVertexPointer(size, type, stride, addr(p)); }
    @Override public void glTexCoordPointer(int size, int type, int stride, Buffer p) { nglTexCoordPointer(size, type, stride, addr(p)); }
    @Override public void glColorPointer(int size, int type, int stride, Buffer p) { nglColorPointer(size, type, stride, addr(p)); }
    @Override public void glDrawArrays(int mode, int first, int count) { org.lwjgl.opengl.GL11.glDrawArrays(mode, first, count); }
    @Override public void glMatrixMode(int m) { org.lwjgl.opengl.GL11.glMatrixMode(m); }
    @Override public void glLoadIdentity() { org.lwjgl.opengl.GL11.glLoadIdentity(); }
    @Override public void glPushMatrix() { org.lwjgl.opengl.GL11.glPushMatrix(); }
    @Override public void glPopMatrix() { org.lwjgl.opengl.GL11.glPopMatrix(); }
    @Override public void glOrthof(float l, float r, float b, float t, float n, float f) { glOrtho(l, r, b, t, n, f); }
    @Override public void glTexParameterf(int target, int pname, float v) { org.lwjgl.opengl.GL11.glTexParameterf(target, pname, v); }
    @Override public void glEnableClientState(int a) { org.lwjgl.opengl.GL11.glEnableClientState(a); }
    @Override public void glDisableClientState(int a) { org.lwjgl.opengl.GL11.glDisableClientState(a); }
    @Override public void glEnable(int c) { org.lwjgl.opengl.GL11.glEnable(c); }
    @Override public void glDisable(int c) { org.lwjgl.opengl.GL11.glDisable(c); }
    @Override public void glColor4f(float r, float g, float b, float a) { org.lwjgl.opengl.GL11.glColor4f(r, g, b, a); }
    @Override public void glColor4x(int r, int g, int b, int a) { org.lwjgl.opengl.GL11.glColor4f(r / 65536f, g / 65536f, b / 65536f, a / 65536f); }
    @Override public void glBindTexture(int target, int tex) { boundTex = tex; org.lwjgl.opengl.GL11.glBindTexture(target, tex); }

    @Override public int glGetError() {
        int e = org.lwjgl.opengl.GL11.glGetError();
        if (e != 0 && debug) System.err.println("[GL] errore 0x" + Integer.toHexString(e));
        // Gli errori desktop (es. enum ES non riconosciuti) non devono scartare le texture.
        return 0;
    }

    @Override public void glGenTextures(int n, int[] t, int off) { for (int i = 0; i < n; i++) t[off + i] = org.lwjgl.opengl.GL11.glGenTextures(); }
    @Override public void glDeleteTextures(int n, int[] t, int off) {
        for (int i = 0; i < n; i++) { org.lwjgl.opengl.GL11.glDeleteTextures(t[off + i]); texSize.remove(t[off + i]); texCrop.remove(t[off + i]); }
    }
    @Override public void glBlendFunc(int s, int d) { org.lwjgl.opengl.GL11.glBlendFunc(s, d); }

    @Override public void glTexImage2D(int target, int level, int ifmt, int w, int h, int border, int fmt, int type, Buffer p) {
        int bpp = (type == GL_UNSIGNED_BYTE) ? (fmt == GL_RGB ? 3 : 4) : 2;
        int need = w * h * bpp;
        ByteBuffer src = (ByteBuffer) p;
        ByteBuffer copy = MemoryUtil.memAlloc(need);
        ByteBuffer dup = src.duplicate();
        dup.limit(Math.min(dup.capacity(), dup.position() + need));
        copy.put(dup);
        while (copy.hasRemaining()) copy.put((byte) 0);
        copy.flip();
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        org.lwjgl.opengl.GL11.glTexImage2D(target, level, ifmt, w, h, border, fmt, type, copy);
        MemoryUtil.memFree(copy);
        noteTexSize(w, h);
    }

    @Override public void glCompressedTexImage2D(int target, int level, int ifmt, int w, int h, int border, int size, Buffer p) {
        ByteBuffer src = (ByteBuffer) p;
        ByteBuffer copy = MemoryUtil.memAlloc(size);
        ByteBuffer dup = src.duplicate();
        dup.limit(Math.min(dup.capacity(), dup.position() + size));
        copy.put(dup).flip();
        GL13.glCompressedTexImage2D(target, level, ifmt, w, h, border, copy);
        MemoryUtil.memFree(copy);
        noteTexSize(w, h);
    }

    @Override public void glCopyTexImage2D(int target, int level, int ifmt, int x, int y, int w, int h, int border) {
        org.lwjgl.opengl.GL11.glCopyTexImage2D(target, level, ifmt, x, y, w, h, border);
        noteTexSize(w, h);
    }
    @Override public void glShadeModel(int m) { org.lwjgl.opengl.GL11.glShadeModel(m); }
    @Override public void glClearColor(float r, float g, float b, float a) { org.lwjgl.opengl.GL11.glClearColor(r, g, b, a); }
    @Override public void glClear(int mask) { org.lwjgl.opengl.GL11.glClear(mask); }
    @Override public void glViewport(int x, int y, int w, int h) { vpW = Math.max(1, w); vpH = Math.max(1, h); org.lwjgl.opengl.GL11.glViewport(x, y, w, h); }
    @Override public void glTexEnvf(int target, int pname, float v) { org.lwjgl.opengl.GL11.glTexEnvf(target, pname, v); }
    @Override public void glHint(int target, int mode) { org.lwjgl.opengl.GL11.glHint(target, mode); }
    @Override public void glAlphaFunc(int f, float ref) { org.lwjgl.opengl.GL11.glAlphaFunc(f, ref); }

    /** GL_TEXTURE_CROP_RECT_OES (0x8B9D): memorizza il rettangolo sorgente per glDrawTexfOES. */
    @Override public void glTexParameteriv(int target, int pname, int[] params, int off) {
        if (pname == 0x8B9D) {
            texCrop.put(boundTex, new int[]{params[off], params[off + 1], params[off + 2], params[off + 3]});
        }
    }

    /** Emula OES_draw_texture: quadrilatero in coordinate finestra (origine in basso a sinistra). */
    @Override public void glDrawTexfOES(float x, float y, float z, float w, float h) {
        int[] size = texSize.get(boundTex);
        int[] crop = texCrop.get(boundTex);
        if (size == null || crop == null) return;
        float tw = size[0], th = size[1];
        float u0 = crop[0] / tw, v0 = crop[1] / th;
        float u1 = (crop[0] + crop[2]) / tw, v1 = (crop[1] + crop[3]) / th;
        org.lwjgl.opengl.GL11.glMatrixMode(GL_PROJECTION);
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glLoadIdentity();
        glOrtho(0, vpW, 0, vpH, -1, 1);
        org.lwjgl.opengl.GL11.glMatrixMode(GL_MODELVIEW);
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glLoadIdentity();
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2f(x, y);
        glTexCoord2f(u1, v0); glVertex2f(x + w, y);
        glTexCoord2f(u1, v1); glVertex2f(x + w, y + h);
        glTexCoord2f(u0, v1); glVertex2f(x, y + h);
        glEnd();
        org.lwjgl.opengl.GL11.glPopMatrix();
        org.lwjgl.opengl.GL11.glMatrixMode(GL_PROJECTION);
        org.lwjgl.opengl.GL11.glPopMatrix();
        org.lwjgl.opengl.GL11.glMatrixMode(GL_MODELVIEW);
    }
}
