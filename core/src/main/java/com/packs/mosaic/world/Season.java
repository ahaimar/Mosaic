package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;

/**
 * The four visual seasons (Task 4). A season is a pure theme overlay: it
 * never touches the grid, so switching seasons keeps every placed building
 * exactly where it is. Each season carries its own ground overlay tint,
 * a baked ground-decoration style (flowers, fallen leaves, snow drifts…)
 * and an ambient particle effect (spring rain + butterflies, summer birds,
 * autumn leaves, winter snow), plus a colour transform used to re-tint
 * vegetation sprites so trees visibly change with the year.
 *
 * Rendering consumes these values via SeasonDecorationFactory,
 * AmbientEffect and BuildingTextureFactory; persistence keys off
 * {@link #getId()}.
 */
public enum Season {

    SPRING("season.spring.name", GameMap.Effect.SPRING,
        new Color(0.60f, 0.80f, 0.55f, 0.10f), Decoration.FLOWERS),
    SUMMER("season.summer.name", GameMap.Effect.BIRD,
        new Color(1f, 0.95f, 0.62f, 0.14f), Decoration.SUNSHINE),
    AUTUMN("season.autumn.name", GameMap.Effect.LEAVES,
        new Color(1f, 0.62f, 0.25f, 0.22f), Decoration.FALLEN),
    WINTER("season.winter.name", GameMap.Effect.SNOW,
        new Color(0.95f, 0.98f, 1f, 0.60f), Decoration.SNOWDRIFT);

    /** Baked-in ground decoration style, consumed by SeasonDecorationFactory. */
    public enum Decoration {
        FLOWERS, SUNSHINE, FALLEN, SNOWDRIFT
    }

    private final String nameKey;
    private final GameMap.Effect effect;
    private final Color overlayColor;
    private final Decoration decoration;

    Season(String nameKey, GameMap.Effect effect, Color overlayColor, Decoration decoration) {
        this.nameKey = nameKey;
        this.effect = effect;
        this.overlayColor = overlayColor;
        this.decoration = decoration;
    }

    /** Stable persistence id (the lowercased enum name). */
    public String getId() {
        return name().toLowerCase();
    }

    public String getNameKey() {
        return nameKey;
    }

    public GameMap.Effect getEffect() {
        return effect;
    }

    /** Translucent full-grid tint drawn over the ground, e.g. white snow cover in winter. */
    public Color getOverlayColor() {
        return overlayColor;
    }

    public Decoration getDecoration() {
        return decoration;
    }

    /** The season new games start in. */
    public static Season getDefault() {
        return SPRING;
    }

    /** Resolves an id from a save file, falling back to the default season. */
    public static Season byId(String id) {
        if (id == null) return getDefault();
        for (Season season : values()) {
            if (season.getId().equals(id)) return season;
        }
        return getDefault();
    }

    /**
     * Colour transform applied to a vegetation building's base colour so
     * trees visibly change with the year: fresh in spring, brighter in
     * summer, amber in autumn, pale snow-dusted in winter.
     */
    public Color tint(Color base) {
        switch (this) {
            case SUMMER: return lighten(base, 0.18f);
            case AUTUMN: return autumn(base);
            case WINTER: return winter(base);
            default:     return new Color(base);
        }
    }

    private static Color autumn(Color c) {
        float r = c.r * 0.5f + c.g * 1.15f;
        float g = c.g * 0.55f;
        float b = c.b * 0.45f;
        return clamp(r, g, b);
    }

    private static Color winter(Color c) {
        float gray = (c.r + c.g + c.b) / 3f;
        float r = gray + (1f - gray) * 0.45f;
        float g = gray + (1f - gray) * 0.52f;
        float b = gray + (1f - gray) * 0.64f;
        return clamp(r, g, b);
    }

    private static Color lighten(Color c, float amt) {
        return clamp(
            c.r + (1f - c.r) * amt,
            c.g + (1f - c.g) * amt,
            c.b + (1f - c.b) * amt);
    }

    private static Color clamp(float r, float g, float b) {
        return new Color(
            Math.max(0f, Math.min(1f, r)),
            Math.max(0f, Math.min(1f, g)),
            Math.max(0f, Math.min(1f, b)),
            1f);
    }
}
