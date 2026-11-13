# Grabatron – versione PC (Windows) in italiano

Porting desktop di **Grabatron 1.5.5** (Future Games of London, 2012) dalla versione Android, con traduzione italiana, tastiera, mouse e controller.

> Repository privato, solo per uso personale. Grabatron, la sua grafica, i suoni e il codice originale sono di Future Games of London / Hutch Games.

## Come funziona il porting
Il gioco è scritto in Java. La logica (`com.fgol.game`, `com.fgol.lib`) non dipende quasi mai da Android, quindi viene usata **così com'è, in bytecode originale**. Viene sostituito solo lo strato che usava Android:

| Originale Android | Versione PC (`src/`) |
|---|---|
| Activity `Host` | `com/fgol/platform/system/Host.java`: tempi di gioco, salvataggi in `%APPDATA%\Grabatron`, risorse, stub di negozio, Facebook e analytics |
| `SoundPool` / `MediaPlayer` | `SoundBank.java`: OpenAL + decodifica OGG con stb_vorbis |
| Touch / tasti / gamepad Android | `Keyb.java`: tastiera, mouse e controller tramite GLFW (database di mappature SDL) |
| `GLSurfaceView` / OpenGL ES 1.x | `desktop/Main.java` (finestra GLFW, ciclo di gioco, schermo intero) e `desktop/DesktopGL.java` (OpenGL ES 1.x → OpenGL desktop, emulazione di `glDrawTexfOES`) |
| `Bitmap`, `GLUtils`, `Build`… | piccole classi sostitutive in `src/android/…` e `src/javax/…` |
| `ImageLoader`, `fgolImage`, `cpuGraphics` | riscritti in `src/com/fgol/platform/graphics/` |

Il **renderer originale** (`SimpleGLRenderer`, `BatchGLRenderer`, `glGraphics` e le primitive) è usato in bytecode originale, non decompilato, così la grafica resta identica a quella del telefono. Un controllo statico (`tools/LinkCheck.java`) ha verificato che tutti i 1.505 riferimenti del codice originale verso lo strato platform si risolvono.

Le risorse (`data/raw`) sono quelle della versione Android italiana, con i pulsanti e le schede già tradotti. `data/raw_ids.txt` collega gli ID Android delle risorse ai file.

## File pronti
Lo zip pronto per Windows è nelle **Releases** del repository: `Grabatron_PC_Windows.zip`. Estrai tutto e avvia `Grabatron.exe`. Java è incluso, non serve installare nulla. Istruzioni e comandi sono in `LEGGIMI.txt`.

## Comandi
| | Tastiera / mouse | Controller |
|---|---|---|
| Muovi | WASD / frecce | Stick sinistro / croce |
| Artiglio | Spazio | A / Croce |
| Arma | F / Ctrl | B, X, Y, RB, RT |
| Lento | Shift | LB, LT |
| Pausa / riprendi | Esc / P | Start / Options |
| Menu | clic del mouse | la freccia si muove con lo stick, A clicca, B indietro |
| Schermo intero | F11 / Alt+Invio | |

## Compilare
```
./build.sh          # produce out/Grabatron_PC_Windows.zip
./run-linux.sh      # avvia il gioco su Linux per le prove
```
Servono JDK 17+, curl, zip e python3. Lo script scarica il Java per Windows (Temurin 21 JRE). Il launcher `Grabatron.exe` è già compilato in `launcher/`; per ricompilarlo usa `REBUILD_LAUNCHER=1 ./build.sh`, che usa zig come compilatore C per Windows.

Collaudo automatico (screenshot e input simulati):
```
java -Dgrab.data=data -Dgrab.test=test/t2.txt -cp "out/Grabatron/game.jar:$(ls lib/*.jar | tr '\n' ':')" desktop.Main
```

## Struttura
```
src/       strato PC (Java) + classi sostitutive di Android
vendor/    grabatron-classes.jar: bytecode originale del gioco (con le patch italiane e tastiera)
           classes.dex: il dex Android da cui e' stato estratto
lib/       LWJGL 3.3.6 (GLFW, OpenGL, OpenAL, stb) con librerie native Windows e Linux
data/      risorse del gioco (texture tradotte, suoni, musiche), icona, tabella ID
launcher/  Grabatron.exe e il suo sorgente C, con l'icona
test/      script per il collaudo automatico
tools/     LinkCheck.java
```

Il progetto Android/Chromebook (smali, traduzione, script delle texture) è nel repository `grabatron-android-ita`.
