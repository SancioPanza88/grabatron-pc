package com.fgol.platform.system;

import com.fgol.game.FrontEnd;
import com.fgol.game.GameApp;
import com.fgol.game.GameScreen;
import com.fgol.lib.gui.control.GuiControl;
import com.fgol.lib.gui.layout.ScreenStack;
import com.fgol.platform.graphics.fgolCanvas;
import com.fgol.platform.graphics.fgolGraphics;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.glfw.GLFWGamepadState;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Input PC: tastiera, mouse e controller (Xbox, PlayStation, Switch Pro, 8BitDo e
 * tutti quelli del database SDL integrato in GLFW). Stessa interfaccia della versione
 * Android: tiltX/tiltY/moving per il movimento, tick() e paint() chiamati dal gioco.
 */
public final class Keyb {
    // bit di direzione: 1=su 2=giu 4=sinistra 8=destra 16=lento
    static int keyBits, padBits, padSlow;
    static int stickX, stickY;
    static boolean wantResume;
    public static boolean cursorOn, cursorInit, clickMode, bWeapon;
    public static int cx, cy;
    private static final GLFWGamepadState pad = GLFWGamepadState.create();
    private static final boolean[] padPrev = new boolean[GLFW_GAMEPAD_BUTTON_LAST + 1];
    private static boolean trigRPrev, trigLPrev;

    private Keyb() {}

    private static int digital() { return keyBits | padBits | padSlow; }

    public static boolean moving() { return (digital() & 15) != 0 || stickX != 0 || stickY != 0; }

    /** true se si sta giocando (non menu, non pausa, non game over). */
    public static boolean inGame() {
        GameScreen gs = GameApp.gameScreen;
        if (gs == null || ScreenStack.currentContainer != gs) return false;
        if (GameScreen.isPaused()) return false;
        return GameScreen.game != null && GameScreen.game.gameState == 2;
    }

    private static int mag(int dx, int dy) {
        int m = (dx != 0 && dy != 0) ? 0x8000 : 0x10000;
        if ((digital() & 16) != 0) m /= 3;
        return m;
    }

    private static int dirX() { int d = digital(), v = 0; if ((d & 8) != 0) v++; if ((d & 4) != 0) v--; return v; }
    private static int dirY() { int d = digital(), v = 0; if ((d & 1) != 0) v++; if ((d & 2) != 0) v--; return v; }

    public static int tiltX() {
        if ((digital() & 15) == 0) return stickX;
        int x = dirX(), y = dirY();
        return x * mag(x, y);
    }

    public static int tiltY() {
        if ((digital() & 15) == 0) return -stickY;
        int x = dirX(), y = dirY();
        return y * mag(x, y);
    }

    // ---------------- azioni ----------------
    private static void press(int key, boolean down) {
        fgolCanvas c = fgolCanvas.instance;
        if (c == null) return;
        if (down) c.keyPressed(key); else c.keyReleased(key);
    }

    private static void setPointer1(int x, int y) {
        fgolCanvas.pointerX[1] = x;
        fgolCanvas.pointerY[1] = y;
    }

    private static void claw(boolean down) {
        if (down) { FrontEnd.dpad_enabled = true; setPointer1(-10000, -10000); }
        press(512, down);
    }

    private static void weapon(boolean down) {
        if (down) {
            GuiControl w = GameScreen.ctrlWeapon;
            if (w == null) return;
            FrontEnd.dpad_enabled = true;
            setPointer1(w.getAbsoluteXCentre(), w.getAbsoluteYCentre());
        }
        press(512, down);
    }

    private static void back(boolean down) {
        if (down && GameScreen.currentDialog != null && GameScreen.currentDialog == GameScreen.menuPause) {
            wantResume = true;
            return;
        }
        press(65536, down);
    }

    private static void padA(boolean down) {
        fgolCanvas c = fgolCanvas.instance;
        if (down) {
            clickMode = false;
            if (inGame()) { claw(true); return; }
            if (!cursorOn || !cursorInit) { cursorOn = true; return; }
            if (c == null) return;
            clickMode = true;
            c.pointerPressed(0, cx, cy);
        } else {
            if (clickMode) {
                clickMode = false;
                if (c != null) c.pointerReleased(0, cx, cy);
            } else claw(false);
        }
    }

    private static void padB(boolean down) {
        if (down) bWeapon = inGame();
        if (bWeapon) weapon(down); else back(down);
    }

    private static void move(int bit, boolean down) {
        if (down) {
            keyBits |= bit;
            FrontEnd.dpad_enabled = true;
            if (!inGame()) cursorOn = true;
        } else keyBits &= ~bit;
    }

    // ---------------- tastiera (callback GLFW) ----------------
    public static void onKey(int key, int action) {
        if (action == GLFW_REPEAT) return;
        boolean down = action == GLFW_PRESS;
        switch (key) {
            case GLFW_KEY_W: move(1, down); break;
            case GLFW_KEY_S: move(2, down); break;
            case GLFW_KEY_A: move(4, down); break;
            case GLFW_KEY_D: move(8, down); break;
            case GLFW_KEY_UP: move(1, down); press(1, down); break;
            case GLFW_KEY_DOWN: move(2, down); press(8, down); break;
            case GLFW_KEY_LEFT: move(4, down); press(2, down); break;
            case GLFW_KEY_RIGHT: move(8, down); press(4, down); break;
            case GLFW_KEY_LEFT_SHIFT: case GLFW_KEY_RIGHT_SHIFT:
                if (down) keyBits |= 16; else keyBits &= ~16; break;
            case GLFW_KEY_SPACE: claw(down); break;
            case GLFW_KEY_F: case GLFW_KEY_LEFT_CONTROL: case GLFW_KEY_RIGHT_CONTROL: weapon(down); break;
            case GLFW_KEY_ESCAPE: case GLFW_KEY_P: case GLFW_KEY_BACKSPACE: back(down); break;
            case GLFW_KEY_ENTER: case GLFW_KEY_KP_ENTER: padA(down); break;
            default: break;
        }
    }

    // ---------------- mouse ----------------
    private static boolean mouseDown;
    public static void onMouseMove(int x, int y) {
        cursorOn = false; // il mouse vero sostituisce il cursore del controller
        if (mouseDown && fgolCanvas.instance != null) fgolCanvas.instance.pointerDragged(0, x, y);
        else { fgolCanvas.pointerX[0] = x; fgolCanvas.pointerY[0] = y; }
    }
    public static void onMouseButton(boolean down, int x, int y) {
        fgolCanvas c = fgolCanvas.instance;
        if (c == null) return;
        cursorOn = false;
        if (down) { mouseDown = true; c.pointerPressed(0, x, y); }
        else if (mouseDown) { mouseDown = false; c.pointerReleased(0, x, y); }
    }

    // ---------------- controller ----------------
    private static int ax(float v) {
        float a = Math.abs(v);
        if (a < 0.2f) return 0;
        float t = Math.min(1f, (a - 0.2f) / 0.8f);
        int r = (int) (t * t * 65536f);
        return v < 0 ? -r : r;
    }

    private static boolean edge(int b, boolean now) {
        boolean prev = padPrev[b];
        padPrev[b] = now;
        return now != prev;
    }

    /** Legge tutti i controller collegati e combina i loro comandi. Chiamato ogni fotogramma. */
    public static void pollGamepads() {
        boolean[] btn = new boolean[GLFW_GAMEPAD_BUTTON_LAST + 1];
        float lx = 0, ly = 0, lt = -1, rt = -1;
        boolean any = false;
        for (int j = GLFW_JOYSTICK_1; j <= GLFW_JOYSTICK_LAST; j++) {
            if (!glfwJoystickPresent(j)) continue;
            if (glfwJoystickIsGamepad(j) && glfwGetGamepadState(j, pad)) {
                any = true;
                ByteBuffer b = pad.buttons();
                for (int i = 0; i < btn.length; i++) btn[i] |= b.get(i) == GLFW_PRESS;
                FloatBuffer a = pad.axes();
                if (Math.abs(a.get(GLFW_GAMEPAD_AXIS_LEFT_X)) > Math.abs(lx)) lx = a.get(GLFW_GAMEPAD_AXIS_LEFT_X);
                if (Math.abs(a.get(GLFW_GAMEPAD_AXIS_LEFT_Y)) > Math.abs(ly)) ly = a.get(GLFW_GAMEPAD_AXIS_LEFT_Y);
                lt = Math.max(lt, a.get(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER));
                rt = Math.max(rt, a.get(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER));
            } else {
                // joystick senza mappatura: assi 0/1 e primi pulsanti
                FloatBuffer a = glfwGetJoystickAxes(j);
                ByteBuffer b = glfwGetJoystickButtons(j);
                if (a != null && a.limit() >= 2) { any = true; lx = a.get(0); ly = a.get(1); }
                if (b != null) {
                    int[] map = {GLFW_GAMEPAD_BUTTON_A, GLFW_GAMEPAD_BUTTON_B, GLFW_GAMEPAD_BUTTON_X, GLFW_GAMEPAD_BUTTON_Y,
                            GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, GLFW_GAMEPAD_BUTTON_BACK, GLFW_GAMEPAD_BUTTON_START};
                    for (int i = 0; i < Math.min(map.length, b.limit()); i++) btn[map[i]] |= b.get(i) == GLFW_PRESS;
                }
            }
        }
        if (!any) { stickX = stickY = 0; padBits = 0; return; }

        stickX = ax(lx);
        stickY = ax(ly);
        int hb = 0;
        if (btn[GLFW_GAMEPAD_BUTTON_DPAD_UP]) hb |= 1;
        if (btn[GLFW_GAMEPAD_BUTTON_DPAD_DOWN]) hb |= 2;
        if (btn[GLFW_GAMEPAD_BUTTON_DPAD_LEFT]) hb |= 4;
        if (btn[GLFW_GAMEPAD_BUTTON_DPAD_RIGHT]) hb |= 8;
        padBits = hb;

        if (edge(GLFW_GAMEPAD_BUTTON_A, btn[GLFW_GAMEPAD_BUTTON_A])) padA(btn[GLFW_GAMEPAD_BUTTON_A]);
        if (edge(GLFW_GAMEPAD_BUTTON_B, btn[GLFW_GAMEPAD_BUTTON_B])) padB(btn[GLFW_GAMEPAD_BUTTON_B]);
        for (int b : new int[]{GLFW_GAMEPAD_BUTTON_X, GLFW_GAMEPAD_BUTTON_Y, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER})
            if (edge(b, btn[b])) weapon(btn[b]);
        for (int b : new int[]{GLFW_GAMEPAD_BUTTON_START, GLFW_GAMEPAD_BUTTON_BACK})
            if (edge(b, btn[b])) back(btn[b]);
        boolean slowBtn = btn[GLFW_GAMEPAD_BUTTON_LEFT_BUMPER];
        boolean lDown = lt > 0.0f, rDown = rt > 0.0f;   // grilletti: -1 a riposo, +1 premuti
        padSlow = (slowBtn || lDown) ? 16 : 0;
        if (rDown != trigRPrev) { trigRPrev = rDown; weapon(rDown); }
        trigLPrev = lDown;

        if (moving()) {
            FrontEnd.dpad_enabled = true;
            if (!inGame() && (stickX != 0 || stickY != 0 || padBits != 0)) cursorOn = true;
        }
    }

    // ---------------- chiamati dal gioco ----------------
    /** Thread di gioco (FrontEnd.process): Esc in pausa = riprendi; cursore nei menu. */
    public static void tick() {
        if (wantResume) {
            wantResume = false;
            if (GameScreen.currentDialog != null && GameScreen.currentDialog == GameScreen.menuPause && GameApp.gameScreen != null)
                GameApp.gameScreen.controlExecuted(GameApp.gameScreen, GameScreen.ctrlMenu);
        }
        if (!cursorOn || inGame()) return;
        int dw = fgolCanvas.displayWidth, dh = fgolCanvas.displayHeight;
        if (!cursorInit) { cx = dw / 2; cy = dh / 2; cursorInit = true; }
        int d = digital();
        int vx = stickX, vy = stickY;
        if ((d & 8) != 0) vx = 0x10000;
        if ((d & 4) != 0) vx = -0x10000;
        if ((d & 2) != 0) vy = 0x10000;
        if ((d & 1) != 0) vy = -0x10000;
        int sx = step(vx, dw), sy = step(vy, dw);
        if ((sx | sy) == 0) return;
        cx = Math.max(0, Math.min(dw - 1, cx + sx));
        cy = Math.max(0, Math.min(dh - 1, cy + sy));
        if (clickMode && fgolCanvas.instance != null) fgolCanvas.instance.pointerDragged(0, cx, cy);
    }

    private static int step(int v, int size) {
        int s = (int) ((long) v * size / (65536L * 55));
        if (s == 0 && v != 0) s = v > 0 ? 1 : -1;
        return s;
    }

    /** Disegna la freccia del cursore sopra tutto (solo nei menu, con tastiera/controller). */
    public static void paint(fgolGraphics g) {
        if (g == null || !cursorOn || !cursorInit || inGame()) return;
        int s = Math.max(12, fgolCanvas.displayHeight / 14);
        int t = s * 3 / 4;
        g.setColor(0xFF000000);
        g.fillTriangle(cx - 2, cy - 4, cx - 2, cy + s + 3, cx + t + 4, cy + t + 1);
        g.setColor(0xFFFFFFFF);
        g.fillTriangle(cx, cy, cx, cy + s, cx + t, cy + t);
    }

    public static void reset() { keyBits = padBits = padSlow = 0; stickX = stickY = 0; }
}
