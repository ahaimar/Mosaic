package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Immutable definition of one friendly animal species (Task 5). Animals are
 * ambient wildlife: they are never placed or stored, they simply live on
 * every map and move with lightweight waypoint behaviour. Each species has
 * its own colour, size, walking speed and a set of behaviour weights that
 * bias how often it wanders, idles, sits, eats or sleeps.
 *
 * Data-only: {@link Animal} consumes the movement/weight values, and
 * AnimalSimulation consumes the colour/size for rendering.
 */
public enum AnimalType {

    DOG("dog", "animal.dog", new Color(0.62f, 0.44f, 0.28f, 1f), 16f, 70f,
        0.10f, 0.35f, 0.20f, 0.20f, 0.05f, 0.10f),
    CAT("cat", "animal.cat", new Color(0.85f, 0.55f, 0.30f, 1f), 13f, 80f,
        0.10f, 0.25f, 0.30f, 0.20f, 0.05f, 0.10f),
    COW("cow", "animal.cow", new Color(0.92f, 0.90f, 0.85f, 1f), 20f, 35f,
        0.04f, 0.22f, 0.14f, 0.03f, 0.40f, 0.17f),
    SHEEP("sheep", "animal.sheep", new Color(0.95f, 0.93f, 0.88f, 1f), 18f, 30f,
        0.04f, 0.22f, 0.14f, 0.03f, 0.35f, 0.22f),
    CHICKEN("chicken", "animal.chicken", new Color(0.93f, 0.92f, 0.90f, 1f), 12f, 55f,
        0.10f, 0.35f, 0.20f, 0.03f, 0.27f, 0.05f),
    RABBIT("rabbit", "animal.rabbit", new Color(0.75f, 0.60f, 0.45f, 1f), 13f, 90f,
        0.10f, 0.40f, 0.20f, 0.18f, 0.07f, 0.05f),
    DUCK("duck", "animal.duck", new Color(0.95f, 0.85f, 0.35f, 1f), 13f, 45f,
        0.10f, 0.40f, 0.20f, 0.03f, 0.22f, 0.05f);

    private final String id;
    private final String nameKey;
    private final Color bodyColor;
    private final float size;
    private final float speed;
    private final float turnWeight;
    private final float wanderWeight;
    private final float idleWeight;
    private final float sitWeight;
    private final float eatWeight;
    private final float sleepWeight;

    AnimalType(String id, String nameKey, Color bodyColor, float size, float speed,
               float turnWeight, float wanderWeight, float idleWeight,
               float sitWeight, float eatWeight, float sleepWeight) {
        this.id = id;
        this.nameKey = nameKey;
        this.bodyColor = bodyColor;
        this.size = size;
        this.speed = speed;
        this.turnWeight = turnWeight;
        this.wanderWeight = wanderWeight;
        this.idleWeight = idleWeight;
        this.sitWeight = sitWeight;
        this.eatWeight = eatWeight;
        this.sleepWeight = sleepWeight;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public Color getBodyColor() { return bodyColor; }
    public float getSize() { return size; }
    /** Walking speed in world units per second while wandering. */
    public float getSpeed() { return speed; }
    public float getTurnWeight() { return turnWeight; }
    public float getWanderWeight() { return wanderWeight; }
    public float getIdleWeight() { return idleWeight; }
    public float getSitWeight() { return sitWeight; }
    public float getEatWeight() { return eatWeight; }
    public float getSleepWeight() { return sleepWeight; }

    /** Sum of every behaviour weight, used to normalise the decision roll. */
    public float getTotalWeight() {
        return turnWeight + wanderWeight + idleWeight + sitWeight + eatWeight + sleepWeight;
    }

    /** Resolves an id, or null if no such species exists. */
    public static AnimalType byId(String id) {
        for (AnimalType type : values()) {
            if (type.getId().equals(id)) return type;
        }
        return null;
    }
}
