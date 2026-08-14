package com.packs.mosaic.world;

import java.util.Random;

/**
 * One living animal (Task 5). A pure, headless-friendly model: it holds a
 * position, heading and a small behaviour state machine driven by
 * lightweight waypoint movement — pick a point in the world, walk to it at
 * the species' speed, then decide what to do next (wander again, idle, turn
 * in place, sit, eat or sleep). Durations are rolled from its own seeded
 * {@link Random}, so behaviour is deterministic per instance.
 *
 * Rendering is entirely separate (AnimalSimulation); this class does no
 * graphics and can be unit-tested on its own.
 */
public final class Animal {

    /** The behaviours an animal can perform. */
    public enum State { WANDER, IDLE, TURN, SIT, EAT, SLEEP }

    private static final float TURN_SPEED = 3.2f;    // radians per second
    private static final float BOUNDS_PAD = 26f;     // keep waypoints off the world edge

    private final AnimalType type;
    private final Random random;
    private final float phase;

    private float x;
    private float y;
    private float heading;
    private float waypointX;
    private float waypointY;
    private float turnTarget;
    private State state;
    private float stateTimer;
    private float stateElapsed;

    public Animal(AnimalType type, float x, float y, long seed) {
        this.type = type;
        this.random = new Random(seed);
        this.x = x;
        this.y = y;
        this.phase = random.nextFloat() * 2f * (float) Math.PI;
        this.heading = random.nextFloat() * 2f * (float) Math.PI;
        setState(State.IDLE, range(0.5f, 2.0f));
    }

    /** Advances the animal one frame; world bounds keep it on the map. */
    public void update(float delta, float worldWidth, float worldHeight) {
        if (delta <= 0f) return;
        stateElapsed += delta;
        stateTimer -= delta;
        switch (state) {
            case WANDER: updateWander(delta, worldWidth, worldHeight); break;
            case TURN:   updateTurn(delta); break;
            case IDLE:
            case SIT:
            case EAT:
            case SLEEP:
                if (stateTimer <= 0f) decideNext(worldWidth, worldHeight);
                break;
            default:
                break;
        }
    }

    private void updateWander(float delta, float worldWidth, float worldHeight) {
        float dx = waypointX - x;
        float dy = waypointY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = type.getSpeed() * delta;
        if (dist <= Math.max(step, 0.01f)) {
            x = waypointX;
            y = waypointY;
            decideNext(worldWidth, worldHeight);
            return;
        }
        heading = (float) Math.atan2(dy, dx);
        x += dx / dist * step;
        y += dy / dist * step;
        x = clamp(x, 0f, worldWidth);
        y = clamp(y, 0f, worldHeight);
    }

    private void updateTurn(float delta) {
        float diff = normalizeAngle(turnTarget - heading);
        float step = TURN_SPEED * delta;
        if (stateTimer <= 0f || Math.abs(diff) <= step) {
            heading = turnTarget;
            setState(State.IDLE, range(0.8f, 2.2f));
            return;
        }
        heading += Math.signum(diff) * step;
    }

    /** Rolls the next behaviour from the species' weights and enters it. */
    private void decideNext(float worldWidth, float worldHeight) {
        float total = type.getTotalWeight();
        float roll = random.nextFloat() * total;
        float cursor = type.getTurnWeight();
        if (roll < cursor) {
            enterTurn();
        } else if (roll < (cursor += type.getWanderWeight())) {
            enterWander(worldWidth, worldHeight);
        } else if (roll < (cursor += type.getIdleWeight())) {
            enterIdle();
        } else if (roll < (cursor += type.getSitWeight())) {
            enterSit();
        } else if (roll < (cursor += type.getEatWeight())) {
            enterEat();
        } else {
            enterSleep();
        }
    }

    private void enterTurn() {
        turnTarget = heading + range(-(float) Math.PI, (float) Math.PI);
        setState(State.TURN, 3f);
    }

    private void enterWander(float worldWidth, float worldHeight) {
        waypointX = range(BOUNDS_PAD, worldWidth - BOUNDS_PAD);
        waypointY = range(BOUNDS_PAD, worldHeight - BOUNDS_PAD);
        setState(State.WANDER, -1f);
    }

    private void enterIdle() {
        setState(State.IDLE, range(1.2f, 4.0f));
    }

    private void enterSit() {
        setState(State.SIT, range(3.0f, 7.0f));
    }

    private void enterEat() {
        setState(State.EAT, range(2.0f, 4.5f));
    }

    private void enterSleep() {
        setState(State.SLEEP, range(6.0f, 15.0f));
    }

    private void setState(State next, float timer) {
        state = next;
        stateTimer = timer;
        stateElapsed = 0f;
    }

    /** Test seam: drops the animal straight into a wander toward a fixed point. */
    void startWanderTo(float targetX, float targetY) {
        waypointX = targetX;
        waypointY = targetY;
        setState(State.WANDER, -1f);
    }

    /** Test seam: forces the animal to turn to an exact heading. */
    void forceTurnTo(float targetHeading) {
        turnTarget = targetHeading;
        setState(State.TURN, 3f);
    }

    public AnimalType getType() { return type; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getHeading() { return heading; }
    public State getState() { return state; }
    public float getStateTimer() { return stateTimer; }
    public float getStateElapsed() { return stateElapsed; }
    /** Per-instance animation phase (0..2PI), fixed at birth. */
    public float getPhase() { return phase; }

    private float range(float min, float max) {
        if (max <= min) return min;
        return min + (max - min) * random.nextFloat();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Wraps an angle into (-PI, PI]. */
    private static float normalizeAngle(float angle) {
        while (angle > (float) Math.PI) angle -= 2f * (float) Math.PI;
        while (angle <= -(float) Math.PI) angle += 2f * (float) Math.PI;
        return angle;
    }
}
