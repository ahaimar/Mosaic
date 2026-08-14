package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 4 — the seasonal system: four visual seasons with their own theme,
 * overlay, ground decoration and ambient effect, resolvable from a save id.
 */
@DisplayName("Season — the four visual seasons")
class SeasonTest {

    @Test
    void registersAllFourSeasons() {
        assertEquals(4, Season.values().length);
    }

    @Test
    void idsAreUniqueAndLowercase() {
        Set<String> ids = new HashSet<>();
        for (Season season : Season.values()) {
            assertEquals(season.name().toLowerCase(), season.getId());
            assertTrue(ids.add(season.getId()), "duplicate id: " + season.getId());
        }
    }

    @Test
    void byIdResolvesEverySeason() {
        for (Season season : Season.values()) {
            assertSame(season, Season.byId(season.getId()));
        }
    }

    @Test
    void unknownAndNullFallBackToSpring() {
        assertEquals(Season.SPRING, Season.byId("monsoon"));
        assertEquals(Season.SPRING, Season.byId(null));
    }

    @Test
    void defaultSeasonIsSpring() {
        assertEquals(Season.SPRING, Season.getDefault());
    }

    @Test
    void everySeasonHasFullThemeData() {
        for (Season season : Season.values()) {
            assertNotNull(season.getNameKey(), season.getId() + " name key");
            assertNotNull(season.getEffect(), season.getId() + " effect");
            assertNotNull(season.getOverlayColor(), season.getId() + " overlay");
            assertNotNull(season.getDecoration(), season.getId() + " decoration");
            assertTrue(season.getNameKey().startsWith("season."), season.getId() + " name key prefix");
        }
    }

    @Test
    void seasonsUseTheExpectedAmbientEffects() {
        assertEquals(GameMap.Effect.SPRING, Season.SPRING.getEffect());
        assertEquals(GameMap.Effect.BIRD, Season.SUMMER.getEffect());
        assertEquals(GameMap.Effect.LEAVES, Season.AUTUMN.getEffect());
        assertEquals(GameMap.Effect.SNOW, Season.WINTER.getEffect());
    }

    @Test
    void seasonsUseTheExpectedDecorations() {
        assertEquals(Season.Decoration.FLOWERS, Season.SPRING.getDecoration());
        assertEquals(Season.Decoration.SUNSHINE, Season.SUMMER.getDecoration());
        assertEquals(Season.Decoration.FALLEN, Season.AUTUMN.getDecoration());
        assertEquals(Season.Decoration.SNOWDRIFT, Season.WINTER.getDecoration());
    }

    @Test
    void springTintKeepsTheBaseColour() {
        Color base = new Color(0.3f, 0.65f, 0.3f, 1f);
        assertEquals(base, Season.SPRING.tint(base));
    }

    @Test
    void summerTintBrightensEveryChannel() {
        Color base = new Color(0.3f, 0.65f, 0.3f, 1f);
        Color tint = Season.SUMMER.tint(base);
        assertNotEquals(base, tint);
        assertTrue(tint.r >= base.r, "red brightened");
        assertTrue(tint.g >= base.g, "green brightened");
        assertTrue(tint.b >= base.b, "blue brightened");
        assertTrue(tint.r > base.r, "red strictly brightened");
    }

    @Test
    void autumnTintShiftsGreenTreesTowardOrange() {
        Color base = new Color(0.3f, 0.65f, 0.3f, 1f);
        Color tint = Season.AUTUMN.tint(base);
        assertTrue(tint.r > base.r, "red grows for autumn");
        assertTrue(tint.g < base.g, "green fades for autumn");
        assertTrue(tint.b < base.b, "blue fades for autumn");
    }

    @Test
    void winterTintWashesColourTowardSnow() {
        Color base = new Color(0.3f, 0.65f, 0.3f, 1f);
        Color tint = Season.WINTER.tint(base);
        assertNotEquals(base, tint);
        float spread = maxComponent(tint) - minComponent(tint);
        assertTrue(spread < maxComponent(base) - minComponent(base), "winter desaturates the base");
    }

    @Test
    void tintIsAlwaysWithinRange() {
        Color base = new Color(0.1f, 0.4f, 0.2f, 1f);
        for (Season season : Season.values()) {
            Color tint = season.tint(base);
            assertTrue(tint.r >= 0f && tint.r <= 1f, "red in range");
            assertTrue(tint.g >= 0f && tint.g <= 1f, "green in range");
            assertTrue(tint.b >= 0f && tint.b <= 1f, "blue in range");
            assertEquals(1f, tint.a);
        }
    }

    @Test
    void overlayColourIsAlwaysTranslucent() {
        for (Season season : Season.values()) {
            assertTrue(season.getOverlayColor().a < 1f, season.getId() + " overlay is translucent");
            assertTrue(season.getOverlayColor().a > 0f, season.getId() + " overlay is visible");
        }
    }

    private static float maxComponent(Color c) {
        return Math.max(c.r, Math.max(c.g, c.b));
    }

    private static float minComponent(Color c) {
        return Math.min(c.r, Math.min(c.g, c.b));
    }
}
