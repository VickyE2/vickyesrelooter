package io.github.vickye2.vickyesrelooter.client.gui.widgets;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Draws a nice configurable check/tick mark.
 */
@OnlyIn(Dist.CLIENT)
public final class GuiUtil {

    /**
     * Draw a checkmark centered at cx,cy.
     *
     * @param gui       GuiGraphics from screen render
     * @param cx        center x in screen coords
     * @param cy        center y in screen coords
     * @param size      overall size (visual bounding box) in pixels
     * @param thickness line thickness in pixels (use 2-6 typically)
     * @param color     ARGB color (0xAARRGGBB)
     */
    public static void drawCheckmark(GuiGraphics gui, float cx, float cy, int size, float thickness, int color) {
        // proportions — tweak these to taste
        float s = size * 0.5f;
        // points relative to center:
        // A -> B is the short downward stroke, B -> C is the long upward stroke
        float ax = -0.55f * s, ay = 0.05f * s;
        float bx = -0.05f * s, by = 0.45f * s;
        float cx2 = 0.75f * s, cy2 = -0.55f * s;

        // draw two thick segments: A->B and B->C
        drawThickSegment(gui, cx + ax, cy + ay, cx + bx, cy + by, thickness, color);
        drawThickSegment(gui, cx + bx, cy + by, cx + cx2, cy + cy2, thickness, color);
    }

    /**
     * Convenience: center the check inside a box defined by top-left (x,y) and size.
     */
    public static void drawCheckmarkInBox(GuiGraphics gui, int boxX, int boxY, int boxSize, float thickness, int color) {
        float cx = boxX + boxSize * 0.5f;
        float cy = boxY + boxSize * 0.5f;
        // make check a bit smaller than the box so it has padding
        int size = Math.max(4, boxSize - 6);
        drawCheckmark(gui, cx, cy, size, thickness, color);
    }

    // ----- internal helper -----

    /**
     * Draws a single thick segment from (x1,y1) to (x2,y2).
     * Uses pose stack transforms so the quad can be drawn as an axis-aligned rectangle.
     */
    private static void drawThickSegment(GuiGraphics gui, double x1, double y1, double x2, double y2, float thickness, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) return;

        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Push transform, move to segment start, rotate to angle
        gui.pose().pushPose();
        gui.pose().translate((float) x1, (float) y1, 0f);
        gui.pose().mulPose(Axis.ZP.rotationDegrees(angleDeg));

        // draw a filled rectangle [0..len] x [-th/2 .. th/2]
        int left = 0;
        int top = (int) Math.floor(-thickness / 2.0);
        int right = (int) Math.ceil(len);
        int bottom = (int) Math.ceil(thickness / 2.0);

        // use GuiGraphics.fill which respects current pose transform
        gui.fill(left, top, right, bottom, color);

        gui.pose().popPose();
    }
}