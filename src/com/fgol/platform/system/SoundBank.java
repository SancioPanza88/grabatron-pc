package com.fgol.platform.system;

import desktop.Res;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * Audio desktop: OpenAL + stb_vorbis. Stessa interfaccia e semantica della
 * versione Android (SoundPool/MediaPlayer): una voce per effetto, loop,
 * volume sinistro/destro, velocita' e dissolvenze.
 */
public class SoundBank {
    public static final int cDefaultVolume = 100;
    public static final int cNumSFXStreams = 8;
    public static boolean soundEnabled = true;
    public static boolean soundLoaded = false;

    private static boolean alOk = false;
    private static long device, context;

    private static int[] musicFiles;
    private static int musicCurrent = -1;
    private static int musicVolume = 100;
    private static int musicSource = 0;
    private static final HashMap<Integer, Integer> musicBuffers = new HashMap<>();

    private static int[] sfxFiles;
    private static int sfxNumFiles = 0;
    private static int[] sfxBuffer, sfxSource, sfxChannels;
    private static int[][] sfxVolume;
    private static boolean[] sfxLooping, sfxPaused, sfxPlaying, sfxFadingOut, sfxActive;
    private static int[] sfxFadeTimeTotal, sfxFadeTimeCurrent, sfxPlayDuration, sfxPlayTimer;

    public SoundBank() {}

    private static void initAL() {
        if (alOk || device != 0) return;
        try {
            device = alcOpenDevice((ByteBuffer) null);
            if (device == 0) { System.err.println("Audio: nessun dispositivo"); device = -1; return; }
            ALCCapabilities caps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            alcMakeContextCurrent(context);
            AL.createCapabilities(caps);
            alOk = true;
        } catch (Throwable t) {
            System.err.println("Audio non disponibile: " + t);
            device = -1;
        }
    }

    /** Decodifica un OGG in un buffer OpenAL; ritorna {buffer, canali, durata ms} o null. */
    private static int[] loadOgg(int resId) {
        try {
            byte[] data = Res.bytesById(resId);
            ByteBuffer mem = MemoryUtil.memAlloc(data.length);
            mem.put(data).flip();
            try (MemoryStack st = MemoryStack.stackPush()) {
                IntBuffer ch = st.mallocInt(1), rate = st.mallocInt(1);
                ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(mem, ch, rate);
                MemoryUtil.memFree(mem);
                if (pcm == null) return null;
                int channels = ch.get(0), sr = rate.get(0);
                int buf = alGenBuffers();
                alBufferData(buf, channels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, sr);
                int ms = (int) (pcm.remaining() / channels * 1000L / sr);
                MemoryUtil.memFree(pcm);
                return new int[]{buf, channels, ms};
            }
        } catch (Exception e) {
            System.err.println("Audio: impossibile caricare 0x" + Integer.toHexString(resId) + ": " + e);
            return null;
        }
    }

    // ---------------- musica ----------------
    public static void musicSetFiles(int[] audioFiles) {
        initAL();
        musicFiles = audioFiles;
        musicVolume = 100;
        musicCurrent = -1;
    }

    public static boolean musicLoad(int idx) {
        if (!alOk || musicFiles == null || idx < 0 || idx >= musicFiles.length) { musicCurrent = -1; return false; }
        Integer buf = musicBuffers.get(idx);
        if (buf == null) {
            int[] r = loadOgg(musicFiles[idx]);
            if (r == null) { musicCurrent = -1; return false; }
            buf = r[0];
            musicBuffers.put(idx, buf);
        }
        if (musicSource == 0) {
            musicSource = alGenSources();
            alSourcei(musicSource, AL_SOURCE_RELATIVE, AL_TRUE);
        }
        alSourceStop(musicSource);
        alSourcei(musicSource, AL_BUFFER, buf);
        musicCurrent = idx;
        return true;
    }

    public static int musicGetCurrent() { return musicCurrent; }
    public static int musicGetVolume() { return musicVolume; }

    public static void musicPlay(int idx, boolean looping, int volume) {
        if (!soundEnabled || !alOk) return;
        if (musicCurrent != idx || musicSource == 0) {
            if (!musicLoad(idx)) return;
        }
        alSourcei(musicSource, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
        alSourcef(musicSource, AL_GAIN, volume * 0.01f);
        if (alGetSourcei(musicSource, AL_SOURCE_STATE) != AL_PLAYING) alSourcePlay(musicSource);
        musicCurrent = idx;
        musicVolume = volume;
    }

    public static void musicStop() {
        if (alOk && musicSource != 0) alSourceStop(musicSource);
        musicCurrent = -1;
    }

    public static void musicSetVolume(int vol) {
        musicVolume = vol;
        if (alOk && musicSource != 0) alSourcef(musicSource, AL_GAIN, vol * 0.01f);
    }

    // ---------------- effetti ----------------
    public static void sfxSetFiles(int[] audioFiles) {
        initAL();
        sfxFiles = audioFiles;
        sfxNumFiles = audioFiles.length;
        int n = sfxNumFiles;
        sfxBuffer = new int[n]; sfxSource = new int[n]; sfxChannels = new int[n];
        sfxVolume = new int[n][2];
        sfxLooping = new boolean[n]; sfxPaused = new boolean[n]; sfxPlaying = new boolean[n];
        sfxFadingOut = new boolean[n]; sfxActive = new boolean[n];
        sfxFadeTimeTotal = new int[n]; sfxFadeTimeCurrent = new int[n];
        sfxPlayDuration = new int[n]; sfxPlayTimer = new int[n];
        for (int i = 0; i < n; i++) sfxLoad(i);
        soundLoaded = true;
    }

    public static boolean sfxLoad(int idx) {
        if (!alOk) return false;
        int[] r = loadOgg(sfxFiles[idx]);
        if (r == null) return false;
        sfxBuffer[idx] = r[0];
        sfxChannels[idx] = r[1];
        sfxPlayDuration[idx] = (r[2] << 16) / 1000;
        int src = alGenSources();
        if (alGetError() != AL_NO_ERROR) return false;
        alSourcei(src, AL_BUFFER, r[0]);
        alSourcei(src, AL_SOURCE_RELATIVE, AL_TRUE);
        sfxSource[idx] = src;
        return true;
    }

    /** Durate gia' calcolate in sfxLoad. */
    public static void sfxGetDurations(Host h) {}

    private static void applyVolume(int idx, float scale) {
        int src = sfxSource[idx];
        if (src == 0) return;
        float l = sfxVolume[idx][0] * 0.01f * scale, r = sfxVolume[idx][1] * 0.01f * scale;
        float gain = Math.max(l, r);
        alSourcef(src, AL_GAIN, gain);
        float pan = (l + r) > 0.0001f ? (r - l) / (l + r) : 0f;
        if (sfxChannels[idx] == 1) alSource3f(src, AL_POSITION, pan, 0f, (float) -Math.sqrt(Math.max(0, 1 - pan * pan)));
    }

    public static void sfxPlay(int idx, boolean looping, int volume, int fpFadeInTime) {
        if (soundLoaded) sfxPlayStereo(idx, looping, volume, volume, fpFadeInTime);
    }

    public static void sfxPlayStereo(int idx, boolean looping, int volL, int volR, int fpFadeInTime) {
        if (!soundLoaded || !soundEnabled || !alOk || idx < 0 || idx >= sfxNumFiles) return;
        int src = sfxSource[idx];
        if (src == 0) return;
        sfxVolume[idx][0] = Math.max(0, Math.min(100, volL));
        sfxVolume[idx][1] = Math.max(0, Math.min(100, volR));
        sfxFadeTimeTotal[idx] = fpFadeInTime;
        sfxFadeTimeCurrent[idx] = 0;
        sfxFadingOut[idx] = false;
        alSourceStop(src);
        alSourcei(src, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);
        alSourcef(src, AL_PITCH, 1f);
        applyVolume(idx, fpFadeInTime > 0 ? 0f : 1f);
        alSourcePlay(src);
        sfxActive[idx] = true;
        if (!looping && sfxPlayDuration[idx] != 0) { sfxPlayTimer[idx] = 0; sfxPlaying[idx] = true; }
        sfxPaused[idx] = false;
        sfxLooping[idx] = looping;
    }

    public static void sfxStop(int idx, int fpFadeOutTime) {
        if (!soundLoaded || idx < 0 || idx >= sfxNumFiles) return;
        if (fpFadeOutTime > 0) {
            sfxFadeTimeTotal[idx] = fpFadeOutTime;
            sfxFadeTimeCurrent[idx] = 0;
            sfxFadingOut[idx] = true;
        } else {
            if (alOk && sfxSource[idx] != 0) alSourceStop(sfxSource[idx]);
            sfxActive[idx] = false;
            sfxPlaying[idx] = false;
            sfxFadeTimeTotal[idx] = 0;
        }
    }

    public static void sfxStopAll() {
        if (!soundLoaded) return;
        for (int i = 0; i < sfxNumFiles; i++) {
            if (alOk && sfxSource[i] != 0) alSourceStop(sfxSource[i]);
            sfxActive[i] = false;
            sfxPlaying[i] = false;
        }
    }

    public static void sfxPause(int idx, boolean p) {
        if (!soundLoaded || !alOk || sfxSource[idx] == 0 || !sfxActive[idx]) return;
        if (p) alSourcePause(sfxSource[idx]); else alSourcePlay(sfxSource[idx]);
        sfxPaused[idx] = p;
    }

    public static boolean sfxIsPaused(int idx) { return soundLoaded && sfxPaused[idx]; }
    public static boolean sfxIsLooping(int idx) { return soundLoaded && sfxLooping[idx] && sfxActive[idx]; }
    public static boolean sfxIsFadingIn(int idx) { return soundLoaded && sfxFadeTimeTotal[idx] > 0 && !sfxFadingOut[idx]; }
    public static boolean sfxIsFadingOut(int idx) { return soundLoaded && sfxFadeTimeTotal[idx] > 0 && sfxFadingOut[idx]; }
    public static boolean sfxIsPlaying(int idx) { return soundLoaded && sfxPlaying[idx]; }

    public static void sfxSetVolume(int idx, int vol) { sfxSetVolumeStereo(idx, vol, vol); }

    public static void sfxSetVolumeStereo(int idx, int volL, int volR) {
        if (!soundLoaded || !alOk || !sfxActive[idx]) return;
        sfxVolume[idx][0] = Math.max(0, Math.min(100, volL));
        sfxVolume[idx][1] = Math.max(0, Math.min(100, volR));
        if (sfxFadeTimeTotal[idx] == 0) applyVolume(idx, 1f);
    }

    public static void sfxSetRate(int idx, int rate) {
        if (!soundLoaded || !alOk || !sfxActive[idx]) return;
        alSourcef(sfxSource[idx], AL_PITCH, Math.max(0.25f, Math.min(4f, rate / 65536f)));
    }

    public static void sfxSaveLoops() {}
    public static void sfxRestoreLoops() {}
    public static void sfxClearSavedLoops() {}

    public static void enableSound(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) { musicStop(); sfxStopAll(); }
    }

    public static void process() {
        if (!soundLoaded || !alOk) return;
        int dt = Host.fp_deltatime;
        for (int i = 0; i < sfxNumFiles; i++) {
            if (sfxPlaying[i]) {
                sfxPlayTimer[i] += dt;
                if (sfxPlayTimer[i] >= sfxPlayDuration[i]) { sfxPlayTimer[i] = 0; sfxPlaying[i] = false; }
            }
            if (sfxFadeTimeTotal[i] > 0) {
                sfxFadeTimeCurrent[i] += dt;
                float t = Math.min(1f, sfxFadeTimeCurrent[i] / (float) sfxFadeTimeTotal[i]);
                if (sfxFadingOut[i]) {
                    if (t >= 1f) { sfxFadeTimeTotal[i] = 0; sfxStop(i, 0); }
                    else applyVolume(i, 1f - t);
                } else {
                    if (t >= 1f) { sfxFadeTimeTotal[i] = 0; applyVolume(i, 1f); }
                    else applyVolume(i, t);
                }
            }
        }
    }

    public static void shutdown() {
        musicStop();
        sfxStopAll();
        musicCurrent = -1;
    }

    /** Chiusura definitiva del dispositivo audio (all'uscita del programma). */
    public static void destroy() {
        if (!alOk) return;
        alcMakeContextCurrent(0);
        alcDestroyContext(context);
        alcCloseDevice(device);
        alOk = false;
    }
}
