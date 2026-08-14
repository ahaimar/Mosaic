package com.packs.mosaic.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.packs.mosaic.world.Animal;
import com.packs.mosaic.world.AnimalType;

/**
 * Ambient wildlife layer (Task 5): a small friendly population of animals
 * lives on every map. They never block building or take part in progression —
 * they just wander around the village with lightweight waypoint movement and
 * perform simple behaviours (idle, turn, sit, eat, sleep). Like
 * {@link AmbientEffect}, this is pure world-space state drawn as small
 * ShapeRenderer shapes, seeded per map so the same world always has the same
 * animals.
 */
public final class AnimalSimulation implements Disposable {

    private static final int POPULATION = 14;
    private static final float SPAWN_PAD = 30f;

    private final Array<Animal> animals = new Array<>();
    private final float worldWidth;
    private final float worldHeight;

    public AnimalSimulation(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    /** Fills the map with a deterministic (per map id) population of animals. */
    public void spawnDefault(String mapId) {
        animals.clear();
        AnimalType[] species = AnimalType.values();
        int baseSeed = mapId == null ? 0 : mapId.hashCode();
        for (int i = 0; i < POPULATION; i++) {
            AnimalType type = species[i % species.length];
            long seed = baseSeed * 31L + i;
            float x = SPAWN_PAD + Math.abs(seed % 7919L) % (long) (worldWidth - 2f * SPAWN_PAD);
            float y = SPAWN_PAD + Math.abs(seed * 2654435761L) % (long) (worldHeight - 2f * SPAWN_PAD);
            animals.add(new Animal(type, x, y, seed));
        }
    }

    public void update(float delta) {
        for (Animal animal : animals) {
            animal.update(delta, worldWidth, worldHeight);
        }
    }

    /** Draws every animal; call inside a Filled ShapeRenderer block. */
    public void render(ShapeRenderer renderer) {
        for (Animal animal : animals) {
            drawAnimal(renderer, animal);
        }
    }

    public Array<Animal> getAnimals() {
        return animals;
    }

    @Override
    public void dispose() {
        // Pure CPU state — nothing to free. Present for symmetry with the
        // screen's dispose chain.
    }

    private static void drawAnimal(ShapeRenderer renderer, Animal animal) {
        AnimalType type = animal.getType();
        Animal.State state = animal.getState();
        float x = animal.getX();
        float y = animal.getY();
        float heading = animal.getHeading();
        float ux = MathUtils.cos(heading);
        float uy = MathUtils.sin(heading);
        float px = -uy;
        float py = ux;
        float size = type.getSize();
        float elapsed = animal.getStateElapsed();
        float phase = animal.getPhase();
        Color body = type.getBodyColor();

        // ground shadow
        renderer.setColor(0f, 0f, 0f, 0.20f);
        renderer.ellipse(x - size * 1.05f, y - size * 0.5f, size * 2.1f, size);

        // breathing / pecking bob
        float bob = 0f;
        if (state == Animal.State.IDLE || state == Animal.State.SIT) {
            bob = MathUtils.sin(elapsed * 3.2f + phase) * size * 0.045f;
        } else if (state == Animal.State.EAT) {
            bob = MathUtils.sin(elapsed * 7f) * size * 0.12f;
        }
        float cx = x;
        float cy = y + bob;

        boolean sleeping = state == Animal.State.SLEEP;
        boolean sitting = state == Animal.State.SIT;
        float bodyL = sleeping ? size * 1.7f : sitting ? size * 0.9f : size * 1.15f;
        float bodyW = sleeping ? size * 0.7f : size * 0.8f;

        // walking legs
        if (state == Animal.State.WANDER) {
            float step = MathUtils.sin(elapsed * 7f + phase) * size * 0.32f;
            renderer.setColor(darken(body, 0.28f));
            renderer.circle(cx + px * bodyW * 0.55f + ux * step, cy + py * bodyW * 0.55f + uy * step, size * 0.22f);
            renderer.circle(cx - px * bodyW * 0.55f - ux * step, cy - py * bodyW * 0.55f - uy * step, size * 0.22f);
        }

        // body capsule along the heading
        renderer.setColor(body);
        renderer.circle(cx, cy, bodyW);
        renderer.circle(cx + ux * bodyL * 0.5f, cy + uy * bodyL * 0.5f, bodyW * 0.8f);
        renderer.circle(cx - ux * bodyL * 0.5f, cy - uy * bodyL * 0.5f, bodyW * 0.8f);

        // head (drops toward the ground when eating)
        float headL = bodyL * 0.55f + bodyW * 0.4f;
        float headR = bodyW * (sitting ? 0.52f : 0.5f);
        float hx = cx + ux * headL;
        float hy = cy + uy * headL;
        if (state == Animal.State.EAT) {
            hx = cx + ux * headL * 0.72f;
            hy = cy + uy * headL * 0.72f - size * 0.25f;
        }
        renderer.setColor(body);
        renderer.circle(hx, hy, headR);

        // per-species features
        switch (type.getId()) {
            case "dog":
                renderer.setColor(darken(body, 0.25f));
                renderer.circle(hx + px * headR * 0.8f, hy + py * headR * 0.8f, headR * 0.45f);
                renderer.circle(hx - px * headR * 0.8f, hy - py * headR * 0.8f, headR * 0.45f);
                renderer.setColor(body);
                renderer.circle(cx - ux * bodyL * 0.9f, cy - uy * bodyL * 0.9f, headR * 0.4f);
                break;
            case "cat":
                renderer.setColor(body);
                renderer.triangle(hx + px * headR * 0.6f, hy + py * headR * 0.6f,
                    hx + px * headR * 0.3f + ux * headR * 0.9f, hy + py * headR * 0.3f + uy * headR * 0.9f,
                    hx + px * headR * 1.1f, hy + py * headR * 1.1f);
                renderer.triangle(hx - px * headR * 0.6f, hy - py * headR * 0.6f,
                    hx - px * headR * 0.3f + ux * headR * 0.9f, hy - py * headR * 0.3f + uy * headR * 0.9f,
                    hx - px * headR * 1.1f, hy - py * headR * 1.1f);
                renderer.setColor(darken(body, 0.2f));
                renderer.circle(cx - ux * bodyL * 0.95f, cy - uy * bodyL * 0.95f, headR * 0.35f);
                break;
            case "cow":
                renderer.setColor(new Color(0.95f, 0.93f, 0.88f, 1f));
                renderer.triangle(hx + px * headR * 0.55f, hy + py * headR * 0.55f,
                    hx + px * headR * 0.75f + ux * headR * 0.7f, hy + py * headR * 0.75f + uy * headR * 0.7f,
                    hx + px * headR * 0.15f + ux * headR * 0.6f, hy + py * headR * 0.15f + uy * headR * 0.6f);
                renderer.triangle(hx - px * headR * 0.55f, hy - py * headR * 0.55f,
                    hx - px * headR * 0.75f + ux * headR * 0.7f, hy - py * headR * 0.75f + uy * headR * 0.7f,
                    hx - px * headR * 0.15f + ux * headR * 0.6f, hy - py * headR * 0.15f + uy * headR * 0.6f);
                renderer.setColor(new Color(0.25f, 0.23f, 0.22f, 1f));
                renderer.circle(cx + px * bodyW * 0.5f, cy + py * bodyW * 0.5f, size * 0.16f);
                renderer.circle(cx - ux * bodyL * 0.45f + px * bodyW * 0.4f,
                    cy - uy * bodyL * 0.45f + py * bodyW * 0.4f, size * 0.14f);
                break;
            case "sheep":
                renderer.setColor(new Color(0.98f, 0.97f, 0.93f, 1f));
                renderer.circle(cx, cy, bodyW * 1.05f);
                renderer.setColor(new Color(0.38f, 0.33f, 0.30f, 1f));
                renderer.circle(hx, hy, headR * 0.9f);
                break;
            case "chicken":
                renderer.setColor(new Color(0.85f, 0.25f, 0.18f, 1f));
                renderer.circle(hx + ux * headR * 0.6f, hy + uy * headR * 0.6f + size * 0.35f, headR * 0.4f);
                renderer.setColor(new Color(0.95f, 0.65f, 0.2f, 1f));
                renderer.triangle(hx + ux * headR * 1.2f, hy + uy * headR * 1.2f,
                    hx + ux * headR * 2.2f, hy + uy * headR * 2.2f - size * 0.3f,
                    hx + ux * headR * 2.2f, hy + uy * headR * 2.2f + size * 0.3f);
                break;
            case "rabbit":
                renderer.setColor(darken(body, 0.1f));
                renderer.circle(hx + px * headR * 0.3f + ux * headR * 0.4f,
                    hy + py * headR * 0.3f + uy * headR * 0.4f + size * 0.7f, headR * 0.35f);
                renderer.circle(hx - px * headR * 0.3f + ux * headR * 0.4f,
                    hy - py * headR * 0.3f + uy * headR * 0.4f + size * 0.7f, headR * 0.35f);
                renderer.setColor(body);
                renderer.circle(hx + px * headR * 0.3f + ux * headR * 0.4f,
                    hy + py * headR * 0.3f + uy * headR * 0.4f + size * 0.7f, headR * 0.22f);
                renderer.circle(hx - px * headR * 0.3f + ux * headR * 0.4f,
                    hy - py * headR * 0.3f + uy * headR * 0.4f + size * 0.7f, headR * 0.22f);
                break;
            case "duck":
                renderer.setColor(new Color(0.95f, 0.55f, 0.2f, 1f));
                renderer.triangle(hx + ux * headR * 1.0f, hy + uy * headR * 1.0f,
                    hx + ux * headR * 1.8f, hy + uy * headR * 1.8f - size * 0.25f,
                    hx + ux * headR * 1.8f, hy + uy * headR * 1.8f + size * 0.25f);
                break;
            default:
                break;
        }

        // sleeping: closed eye
        if (state == Animal.State.SLEEP) {
            renderer.setColor(0.25f, 0.22f, 0.20f, 1f);
            renderer.rectLine(hx - headR * 0.4f, hy, hx + headR * 0.4f, hy, 2f);
        }
    }

    private static Color darken(Color c, float amt) {
        return new Color(c.r * (1f - amt), c.g * (1f - amt), c.b * (1f - amt), 1f);
    }
}
