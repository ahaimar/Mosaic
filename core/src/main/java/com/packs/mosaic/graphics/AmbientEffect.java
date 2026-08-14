package com.packs.mosaic.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.packs.mosaic.world.GameMap;

/**
 * Lightweight world-space particle layer (Task 3) that gives each map a
 * signature atmosphere: falling snow, drifting leaves, rising volcano
 * embers, sea mist, tropical rain or beach sun-glints. Particles are pure
 * floats in world coordinates, updated with a fixed seed so the look is
 * deterministic per map, and drawn as small ShapeRenderer shapes — no
 * textures involved.
 */
public final class AmbientEffect implements Disposable {

    private static final int MAX = 96;

    private GameMap.Effect effect = GameMap.Effect.NONE;
    private float worldWidth;
    private float worldHeight;

    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] speed = new float[MAX];
    private final float[] phase = new float[MAX];
    private final float[] size = new float[MAX];
    private int count;
    /** First index of the butterfly group in spring (rain occupies the indices before it). */
    private int butterflyStart = -1;

    public AmbientEffect(GameMap.Effect effect, float worldWidth, float worldHeight) {
        set(effect, worldWidth, worldHeight);
    }

    /** (Re)configures the effect; cheap enough to call when entering a map. */
    public void set(GameMap.Effect effect, float worldWidth, float worldHeight) {
        this.effect = effect == null ? GameMap.Effect.NONE : effect;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        butterflyStart = -1;
        switch (this.effect) {
            case SNOW:    count = 60; break;
            case LEAVES:  count = 40; break;
            case EMBERS:  count = 50; break;
            case RAIN:    count = 90; break;
            case GLINTS:  count = 28; break;
            case MIST:    count = 9;  break;
            case SPRING:  count = 74; butterflyStart = 60; break;
            case BIRD:    count = 5;  break;
            default:      count = 0;  break;
        }
        MathUtils.random.setSeed(effectHash(this.effect));
        for (int i = 0; i < count; i++) {
            x[i] = MathUtils.random(worldWidth);
            y[i] = i >= butterflyStart && butterflyStart >= 0
                ? MathUtils.random(worldHeight * 0.4f)
                : MathUtils.random(worldHeight);
            phase[i] = MathUtils.random(2f * (float) Math.PI);
            size[i] = effect == GameMap.Effect.MIST
                ? 30f + MathUtils.random(70f)
                : 1.5f + MathUtils.random(2.5f);
            speed[i] = speedFor(this.effect);
        }
        if (butterflyStart >= 0) {
            for (int i = butterflyStart; i < count; i++) speed[i] = 12f;
        }
    }

    private static float speedFor(GameMap.Effect effect) {
        switch (effect) {
            case SNOW:   return 14f;
            case LEAVES: return 26f;
            case EMBERS: return 22f;
            case RAIN:   return 340f;
            case MIST:   return 6f;
            case SPRING: return 340f;
            case BIRD:   return 45f;
            default:     return 0f;
        }
    }

    /** Advances every particle; called once per frame from the screen. */
    public void update(float delta) {
        if (effect == GameMap.Effect.GLINTS || effect == GameMap.Effect.NONE) return;
        float t = delta * 60f;
        for (int i = 0; i < count; i++) {
            switch (effect) {
                case SNOW:
                    y[i] -= speed[i] * t * 0.016f;
                    x[i] += MathUtils.sin(MathUtils.degreesToRadians * (phase[i] + elapsed) * 60f) * 0.3f;
                    if (y[i] < -4f) { y[i] = worldHeight + 4f; x[i] = MathUtils.random(worldWidth); }
                    break;
                case LEAVES:
                    y[i] -= speed[i] * t * 0.016f;
                    x[i] -= 0.4f * t * 0.016f;
                    x[i] += MathUtils.sin(MathUtils.degreesToRadians * (phase[i] + elapsed) * 80f) * 0.5f;
                    if (y[i] < -4f) { y[i] = worldHeight + 4f; x[i] = MathUtils.random(worldWidth); }
                    break;
                case EMBERS:
                    y[i] += speed[i] * t * 0.016f;
                    x[i] += MathUtils.sin(MathUtils.degreesToRadians * (phase[i] + elapsed) * 70f) * 0.4f;
                    if (y[i] > worldHeight + 4f) { y[i] = -4f; x[i] = MathUtils.random(worldWidth); }
                    break;
                case RAIN:
                    y[i] -= speed[i] * t * 0.016f;
                    x[i] -= 0.9f * t * 0.016f;
                    if (y[i] < -4f) { y[i] = worldHeight + 4f; x[i] = MathUtils.random(worldWidth); }
                    break;
                case MIST:
                    x[i] += speed[i] * t * 0.016f;
                    if (x[i] - size[i] > worldWidth) x[i] = -size[i];
                    break;
                case SPRING:
                    if (i < butterflyStart) {
                        y[i] -= speed[i] * t * 0.016f;
                        x[i] -= 0.9f * t * 0.016f;
                        if (y[i] < -4f) { y[i] = worldHeight + 4f; x[i] = MathUtils.random(worldWidth); }
                    } else {
                        y[i] += speed[i] * t * 0.016f;
                        x[i] += MathUtils.sin(MathUtils.degreesToRadians * (phase[i] + elapsed) * 40f) * 0.7f;
                        if (y[i] > worldHeight + 6f) {
                            y[i] = MathUtils.random(worldHeight * 0.35f);
                            x[i] = MathUtils.random(worldWidth);
                        }
                    }
                    break;
                case BIRD:
                    x[i] += speed[i] * t * 0.016f;
                    if (x[i] > worldWidth + 10f) {
                        x[i] = -10f;
                        y[i] = worldHeight * 0.5f + MathUtils.random(worldHeight * 0.35f);
                    }
                    break;
                default:
                    break;
            }
        }
        elapsed += delta;
    }

    private float elapsed;

    /** Draws the particles in world space; call inside a Filled ShapeRenderer block. */
    public void render(ShapeRenderer renderer) {
        if (count == 0) return;
        switch (effect) {
            case SNOW:
                renderer.setColor(0.95f, 0.97f, 1f, 0.9f);
                for (int i = 0; i < count; i++) renderer.circle(x[i], y[i], size[i]);
                break;
            case LEAVES:
                for (int i = 0; i < count; i++) {
                    if ((int) phase[i] % 2 == 0) {
                        renderer.setColor(0.35f, 0.58f, 0.25f, 0.9f);
                    } else {
                        renderer.setColor(0.55f, 0.45f, 0.22f, 0.9f);
                    }
                    renderer.rect(x[i], y[i], size[i], size[i] * 0.7f);
                }
                break;
            case EMBERS:
                for (int i = 0; i < count; i++) {
                    float a = 0.55f + 0.45f * MathUtils.sin(phase[i] + elapsed * 4f);
                    renderer.setColor(0.95f, 0.45f, 0.18f, Math.max(0.25f, a));
                    renderer.circle(x[i], y[i], size[i]);
                }
                break;
            case RAIN:
                renderer.setColor(0.75f, 0.85f, 0.95f, 0.55f);
                for (int i = 0; i < count; i++) {
                    renderer.rectLine(x[i], y[i], x[i] - 2f, y[i] - 10f, 1f);
                }
                break;
            case GLINTS:
                for (int i = 0; i < count; i++) {
                    float a = 0.35f + 0.65f * (0.5f + 0.5f * MathUtils.sin(phase[i] + elapsed * 2.4f));
                    renderer.setColor(1f, 0.95f, 0.75f, a);
                    renderer.circle(x[i], y[i], 1.6f);
                }
                break;
            case MIST:
                for (int i = 0; i < count; i++) {
                    renderer.setColor(0.85f, 0.90f, 0.95f, 0.10f + 0.05f * MathUtils.sin(phase[i] + elapsed));
                    renderer.circle(x[i], y[i], size[i]);
                }
                break;
            case SPRING:
                renderer.setColor(0.75f, 0.85f, 0.95f, 0.45f);
                for (int i = 0; i < butterflyStart; i++) {
                    renderer.rectLine(x[i], y[i], x[i] - 2f, y[i] - 9f, 1f);
                }
                for (int i = butterflyStart; i < count; i++) {
                    int tone = (int) phase[i] % 3;
                    if (tone == 0) {
                        renderer.setColor(0.95f, 0.55f, 0.75f, 0.95f);
                    } else if (tone == 1) {
                        renderer.setColor(0.95f, 0.75f, 0.35f, 0.95f);
                    } else {
                        renderer.setColor(0.55f, 0.75f, 0.95f, 0.95f);
                    }
                    float flutter = MathUtils.sin(phase[i] + elapsed * 5f) * 2f;
                    renderer.circle(x[i], y[i] + flutter, 2.2f);
                }
                break;
            case BIRD:
                renderer.setColor(0.18f, 0.20f, 0.25f, 0.95f);
                for (int i = 0; i < count; i++) {
                    float flap = 5f + 3f * MathUtils.sin(phase[i] + elapsed * 8f);
                    float bx = x[i];
                    float by = y[i];
                    renderer.triangle(bx, by, bx - flap, by + 5f, bx - 2f, by + 1f);
                    renderer.triangle(bx, by, bx + flap, by + 5f, bx + 2f, by + 1f);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        // Pure CPU state — nothing to free. Present for symmetry with the
        // screen's dispose chain.
    }

    private static long effectHash(GameMap.Effect effect) {
        long h = 1125899906842597L;
        String name = effect.name();
        for (int i = 0; i < name.length(); i++) {
            h = 31 * h + name.charAt(i);
        }
        return h;
    }
}
