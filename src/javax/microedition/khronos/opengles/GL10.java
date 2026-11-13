package javax.microedition.khronos.opengles;
import java.nio.Buffer;
/** Sottoinsieme di GL10 (OpenGL ES 1.0) usato dal gioco. */
public interface GL10 {
    void glVertexPointer(int size, int type, int stride, Buffer p);
    void glTexCoordPointer(int size, int type, int stride, Buffer p);
    void glColorPointer(int size, int type, int stride, Buffer p);
    void glDrawArrays(int mode, int first, int count);
    void glMatrixMode(int m);
    void glLoadIdentity();
    void glPushMatrix();
    void glPopMatrix();
    void glOrthof(float l, float r, float b, float t, float n, float f);
    void glTexParameterf(int target, int pname, float v);
    void glEnableClientState(int a);
    void glDisableClientState(int a);
    void glEnable(int c);
    void glDisable(int c);
    void glColor4f(float r, float g, float b, float a);
    void glColor4x(int r, int g, int b, int a);
    void glBindTexture(int target, int tex);
    int glGetError();
    void glGenTextures(int n, int[] t, int off);
    void glDeleteTextures(int n, int[] t, int off);
    void glBlendFunc(int s, int d);
    void glTexImage2D(int target, int level, int ifmt, int w, int h, int border, int fmt, int type, Buffer p);
    void glCompressedTexImage2D(int target, int level, int ifmt, int w, int h, int border, int size, Buffer p);
    void glCopyTexImage2D(int target, int level, int ifmt, int x, int y, int w, int h, int border);
    void glShadeModel(int m);
    void glClearColor(float r, float g, float b, float a);
    void glClear(int mask);
    void glViewport(int x, int y, int w, int h);
    void glTexEnvf(int target, int pname, float v);
    void glHint(int target, int mode);
    void glAlphaFunc(int f, float ref);
}
