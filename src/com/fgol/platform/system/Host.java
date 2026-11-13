package com.fgol.platform.system;

import com.facebook.android.Facebook;
import com.fgol.platform.display.GLGameView;
import com.fgol.platform.graphics.fgolCanvas;
import desktop.Res;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Versione desktop di Host (sull'originale era l'Activity Android).
 * Stessa interfaccia pubblica usata dal codice del gioco.
 */
public abstract class Host {
    public static final String GFX_16B_EXTENSION = "_16b";
    public static final String GFX_ATI_EXTENSION = "_ati";
    public static final String GFX_DEFAULT_EXTENSION = "_16b";
    public static final String GFX_DXT_EXTENSION = "_dxt";
    public static final String GFX_PVR_EXTENSION = "_pvr";
    public static final String TAG = "Grabatron";
    public static final int cMinFrameTime = 10;
    public static final int eLandscape = 0;
    public static final int ePortrait = 1;

    public static String GFX_EXTENSION = "_16b";
    public static boolean applicationPaused = false;
    public static boolean billingAvailable = false;
    public static boolean crapDevice = false;
    public static int defaultOrientation = 0;
    public static boolean doFinishedLoading = false;
    public static boolean dodgyCalibratePosition = false;
    public static boolean dodgyCurrentSensorPosition = false;
    public static int fp_deltatime = 0;
    public static boolean hasFocus = true;
    public static Host instance;
    public static boolean isATI = false;
    public static boolean isDXT = false;
    public static boolean isLoading = false;
    public static boolean isPVR = false;
    public static int lastFrameTime = 20;
    public static boolean loaded = false;
    public static float loadingPercent = 0f;
    public static float[] sensorTilt = new float[3];
    public static String strResourcePackage = null;

    public Facebook mFacebook = null;
    public GLGameView view;
    private long timeLastFrame;

    public Host() {
        instance = this;
    }

    public abstract void init();
    public abstract void pause();
    public abstract void process();
    protected abstract void purchaseComplete(String str);
    protected abstract void purchasesUpdated();
    public abstract void resume();

    public void startup() {
        init();
        loaded = true;
        timeLastFrame = System.currentTimeMillis();
        hasFocus = true;
    }

    public void processInternal() {
        if (applicationPaused) {
            timeLastFrame = System.currentTimeMillis();
            return;
        }
        long time = System.currentTimeMillis();
        if (isLoading) {
            isLoading = false;
            timeLastFrame = time;
        }
        lastFrameTime = (int) (time - timeLastFrame);
        timeLastFrame = time;
        int i = 10;
        if (lastFrameTime >= 10) i = lastFrameTime > 67 ? 67 : lastFrameTime;
        lastFrameTime = i;
        fp_deltatime = lastFrameTime * 66;
        SoundBank.process();
        if (hasFocus) {
            fgolCanvas.beginInputProcessing();
            try {
                process();
            } catch (Exception e) {
                e.printStackTrace();
            }
            fgolCanvas.endInputProcessing();
        }
    }

    public static void setAppLoading() { isLoading = true; }
    public static void setAppLoading(float pc) { loadingPercent = pc; }

    public String getVersionString() { return "1.5.5-PC"; }

    public void finish() {
        try { pause(); } catch (Exception e) { e.printStackTrace(); }
        desktop.Main.requestExit();
    }

    // --- analytics: non piu' esistenti ---
    public static void flurryLog(String s) {}
    public static void flurryLog(String a, String b, String c) {}
    public static void flurryLogBeginTimed(String s) {}
    public static void flurryLogEndTimed(String s) {}
    public static void apsalarEvent(String s) {}
    public static void apsalarEvent(String s, String k, float v) {}

    public static final void gc() { System.gc(); }

    // --- file e risorse ---
    public static File saveDir() {
        String appdata = System.getenv("APPDATA");
        File dir = appdata != null ? new File(appdata, "Grabatron") : new File(System.getProperty("user.home"), ".grabatron");
        dir.mkdirs();
        return dir;
    }

    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return new FileInputStream(new File(saveDir(), name));
    }

    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(new File(saveDir(), name), (mode & 0x8000) != 0);
    }

    public static InputStream openResourceAsStream(String name) {
        int dot = name.indexOf(".");
        if (dot == -1) dot = name.length();
        String basename = name.substring(0, dot);
        File f = Res.file(basename);
        if (f == null) f = Res.file(basename + GFX_EXTENSION);
        if (f == null) f = Res.file(basename + "_16b");
        if (f == null) return null;
        try {
            return new java.io.BufferedInputStream(new FileInputStream(f), 1 << 16);
        } catch (Exception e) {
            return null;
        }
    }

    public InputStream openFileNative(String name) { return openResourceAsStream(name); }

    public void openURL(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("Impossibile aprire " + url);
        }
    }

    // --- acquisti: il negozio originale non esiste piu' ---
    protected static int tryBuyItem(String item) { return -1; }
    public static void restorePurchases() {}
    protected void initBilling() {}

    // --- Facebook: non disponibile ---
    public boolean initFacebook() { return false; }
    public void showFacebookLogin() {}
    public void publishScoreOnFb(int score) {}
    public void publishLikeOnFb(String s) {}

    // --- sensori/vibrazione: non presenti su PC ---
    public void vibrate(int ms) {}
    public static void calibrateSensor() {}
    public static void calibrateSensor30Degrees() {}
    public static void calibrateSensor45Degrees() {}
    public static void calibrateSensorFlat() {}
    public void checkCrapDevice() {}
    public void finishedLoading() {}
    public void updateSensor(float x, float y, float z) {}
}
