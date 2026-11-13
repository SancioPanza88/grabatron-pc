package desktop;

import com.fgol.platform.display.GLGameView;
import com.fgol.platform.display.SimpleGLRenderer;
import com.fgol.platform.graphics.fgolCanvas;
import com.fgol.platform.system.Host;
import com.fgol.platform.system.Keyb;
import com.fgol.platform.system.SoundBank;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/** Avvio della versione PC di Grabatron. */
public final class Main {
    private static long window;
    private static volatile boolean exitRequested;
    private static int winW = 1280, winH = 720, fbW = 1280, fbH = 720;
    private static boolean fullscreen;
    private static int savedX, savedY, savedW, savedH;

    public static void requestExit() { exitRequested = true; }

    public static void main(String[] args) throws Exception {
        File data = new File(System.getProperty("grab.data", "data"));
        if (!new File(data, "raw").isDirectory()) {
            File jarDir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            data = new File(jarDir, "data");
        }
        Res.init(data);
        boolean startFull = false;
        for (String a : args) if (a.equalsIgnoreCase("--fullscreen")) startFull = true;

        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Impossibile inizializzare GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        window = glfwCreateWindow(winW, winH, "Grabatron", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) throw new IllegalStateException("Impossibile creare la finestra (serve OpenGL 2.1)");
        setIcon(new File(data, "icon.png"));

        GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vm != null) glfwSetWindowPos(window, (vm.width() - winW) / 2, (vm.height() - winH) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        DesktopGL gl = new DesktopGL();

        glfwSetKeyCallback(window, (w, key, sc, action, mods) -> {
            if (action == GLFW_PRESS && (key == GLFW_KEY_F11 || (key == GLFW_KEY_ENTER && (mods & GLFW_MOD_ALT) != 0))) {
                toggleFullscreen();
                return;
            }
            if (Host.loaded) Keyb.onKey(key, action);
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> { if (Host.loaded) Keyb.onMouseMove(toCanvasX(x), toCanvasY(y)); });
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (!Host.loaded || button != GLFW_MOUSE_BUTTON_LEFT) return;
            double[] x = new double[1], y = new double[1];
            glfwGetCursorPos(window, x, y);
            Keyb.onMouseButton(action == GLFW_PRESS, toCanvasX(x[0]), toCanvasY(y[0]));
        });
        glfwSetWindowSizeCallback(window, (w, width, height) -> { winW = Math.max(1, width); winH = Math.max(1, height); });
        glfwSetWindowIconifyCallback(window, (w, iconified) -> {
            if (!Host.loaded) return;
            if (iconified) { Host.instance.pause(); Host.applicationPaused = true; Keyb.reset(); }
            else { Host.instance.resume(); Host.applicationPaused = false; }
        });
        glfwSetWindowFocusCallback(window, (w, focused) -> { if (!focused) Keyb.reset(); });

        // schermata nera mentre il gioco carica
        glfwShowWindow(window);
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        glfwSwapBuffers(window);
        glfwPollEvents();

        // avvio del gioco (come il LoadingThread originale)
        Host host = new com.fgol.grabatron1.Grabatron();
        GLGameView view = new GLGameView();
        host.view = view;
        host.startup();
        // su PC non c'e' l'inclinazione: comandi D-pad/tastiera/controller, senza schermata di scelta
        com.fgol.game.FrontEnd.choosenControls = true;
        com.fgol.game.FrontEnd.dpad_enabled = true;
        SimpleGLRenderer r = view.renderer;
        r.onSurfaceCreated(gl, null);
        updateFramebuffer();
        r.onSurfaceChanged(gl, fbW, fbH);
        if (startFull) toggleFullscreen();

        int lastFbW = fbW, lastFbH = fbH;
        TestDriver test = TestDriver.fromProperties();
        long frame = 0;
        while (!glfwWindowShouldClose(window) && !exitRequested) {
            glfwPollEvents();
            updateFramebuffer();
            if (fbW > 0 && fbH > 0 && (fbW != lastFbW || fbH != lastFbH)) {
                lastFbW = fbW; lastFbH = fbH;
                r.onSurfaceChanged(gl, fbW, fbH);
            }
            if (fbW == 0 || fbH == 0) { try { Thread.sleep(50); } catch (InterruptedException e) {} continue; }
            Keyb.pollGamepads();
            glClear(GL_COLOR_BUFFER_BIT);
            r.onDrawFrame(gl);
            if (test != null && test.onFrame(frame++, fbW, fbH)) exitRequested = true;
            glfwSwapBuffers(window);
        }
        try { host.pause(); } catch (Exception e) { e.printStackTrace(); }  // salvataggio automatico
        SoundBank.destroy();
        glfwDestroyWindow(window);
        glfwTerminate();
        System.exit(0);
    }

    private static void updateFramebuffer() {
        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer w = st.mallocInt(1), h = st.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbW = w.get(0); fbH = h.get(0);
        }
    }

    private static int toCanvasX(double x) { return (int) (x * fgolCanvas.displayWidth / Math.max(1, winW)); }
    private static int toCanvasY(double y) { return (int) (y * fgolCanvas.displayHeight / Math.max(1, winH)); }

    private static void toggleFullscreen() {
        long mon = glfwGetPrimaryMonitor();
        GLFWVidMode vm = glfwGetVideoMode(mon);
        if (vm == null) return;
        if (!fullscreen) {
            int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
            glfwGetWindowPos(window, x, y);
            glfwGetWindowSize(window, w, h);
            savedX = x[0]; savedY = y[0]; savedW = w[0]; savedH = h[0];
            glfwSetWindowMonitor(window, mon, 0, 0, vm.width(), vm.height(), vm.refreshRate());
        } else {
            glfwSetWindowMonitor(window, MemoryUtil.NULL, savedX, savedY, savedW, savedH, GLFW_DONT_CARE);
        }
        fullscreen = !fullscreen;
        glfwSwapInterval(1);
    }

    private static void setIcon(File f) {
        if (!f.isFile()) return;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer px = MemoryUtil.memAlloc(w * h * 4);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int p = img.getRGB(x, y);
                px.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
            }
            px.flip();
            try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
                icons.position(0).width(w).height(h).pixels(px);
                glfwSetWindowIcon(window, icons);
            }
            MemoryUtil.memFree(px);
        } catch (Exception e) {
            // icona facoltativa
        }
    }
}
