package android.opengl;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
public class GLUtils {
    public static void texImage2D(int target, int level, Bitmap b, int border) {
        ByteBuffer buf = MemoryUtil.memAlloc(b.width * b.height * 4);
        for (int p : b.pixels) {
            buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
        }
        buf.flip();
        GL11.glTexImage2D(target, level, GL11.GL_RGBA, b.width, b.height, border, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        desktop.DesktopGL.noteTexSize(b.width, b.height);
        MemoryUtil.memFree(buf);
    }
}
