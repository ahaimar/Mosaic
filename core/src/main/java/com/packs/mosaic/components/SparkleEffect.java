package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * A one-shot burst of radiating sparkles, used as celebratory feedback
 * when a challenge is completed. Call {@link #play()} after adding it to
 * the stage; it animates for ~0.9s and then quietly stops (it is removed
 * from its parent when the animation ends).
 */
public class SparkleEffect extends Actor {

    private static final float DURATION = 0.9f;
    private static final int SPARKLES = 14;
    private static final ShapeRenderer RENDERER = new ShapeRenderer();

    private final Vector2 stagePosition = new Vector2();
    private final float[] angle = new float[SPARKLES];
    private final float[] speed = new float[SPARKLES];
    private final float[] size = new float[SPARKLES];
    private float time;

    public SparkleEffect() {
        setSize(140f, 140f);
        for (int i = 0; i < SPARKLES; i++) {
            angle[i] = MathUtils.random(0f, MathUtils.PI2);
            speed[i] = MathUtils.random(70f, 150f);
            size[i] = MathUtils.random(3f, 7f);
        }
    }

    /** Starts the burst. The actor must already be on the stage. */
    public void play() {
        time = 0.0001f;
    }

    public boolean isActive() {
        return time > 0f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (time > 0f) {
            time += delta;
            if (time > DURATION) {
                time = 0f;
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (time <= 0f) {
            return;
        }
        localToStageCoordinates(stagePosition.set(0f, 0f));
        float cx = stagePosition.x + getWidth() / 2f;
        float cy = stagePosition.y + getHeight() / 2f;
        float progress = time / DURATION;

        batch.end();
        RENDERER.setProjectionMatrix(batch.getProjectionMatrix());
        RENDERER.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < SPARKLES; i++) {
            float distance = speed[i] * progress;
            float x = cx + MathUtils.cos(angle[i]) * distance;
            float y = cy + MathUtils.sin(angle[i]) * distance;
            float fade = 1f - progress;
            RENDERER.setColor(1f, 1f, 0.85f, fade * 0.9f);
            RENDERER.circle(x, y, Math.max(1.5f, size[i] * (1f - progress * 0.5f)));
        }
        RENDERER.end();
        batch.begin();
    }
}
