package com.fgol.platform.graphics.primitives;

import com.fgol.platform.graphics.fgolGraphics;
import com.fgol.platform.graphics.glGraphics;

/* loaded from: /home/claude/work/vcheck/classes.dex */
public class PODModel {
    public void destroy() {}

    public void loadPOD(String str) {}

    public void loadTextures() {}

    public void render(int i) {}

    public void setCameraFOV(int i) {}

    public void setCameraFrom(float f, float f2, float f3) {}

    public void setCameraTo(float f, float f2, float f3) {}

    public void setCameraUp(float f, float f2, float f3) {}

    public void setLightAmbient(int i, float f, float f2, float f3) {}

    public void setLightDiffuse(int i, float f, float f2, float f3) {}

    public void setLightDirection(int i, float f, float f2, float f3) {}

    public void setLightEnabled(int i, boolean z) {}

    public void setLightPosition(int i, float f, float f2, float f3) {}

    public void setLightSpecular(int i, float f, float f2, float f3) {}

    public void setLightType(int i, int i2) {}

    public void setMaterial(int i) {}

    public void setOrientation(float f, float f2, float f3, float f4) {}

    public void setPosition(float f, float f2, float f3) {}

    public void setScale(float f, float f2, float f3) {}

    public void setTexture(int i, String str) {}

    public void unloadTextures() {}

    public void paint(fgolGraphics g) {
        glGraphics.drawList.addElement(this);
    }

    public void setPixelShader(String shader) {
    }

    public void setVertexShader(String shader) {
    }

    public void playAnimation(int id) {
    }

    public void updateAnimation(float t) {
    }
}
