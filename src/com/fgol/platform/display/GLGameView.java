package com.fgol.platform.display;

import android.content.Context;
import com.fgol.platform.graphics.fgolCanvas;
import com.fgol.platform.graphics.glGraphics;
import com.fgol.platform.system.Host;

/** Vista di gioco desktop: aggiorna la logica e riempie la lista di disegno. */
public class GLGameView {
    public final glGraphics g = new glGraphics();
    public final BatchGLRenderer renderer;

    public GLGameView() {
        renderer = new BatchGLRenderer(new Context(), this);
        renderer.setSprites(glGraphics.drawList);
    }

    public void process() {
        if (Host.hasFocus) {
            Host.instance.processInternal();
            glGraphics.setCanvas(null);
            fgolCanvas.instance.paint(g);
        }
    }
}
