package com.packs.mosaic.world;

import com.packs.mosaic.graphics.AnimalSimulation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 5 — the friendly animal system: seven non-threatening species that
 * wander the world with lightweight waypoint movement and perform simple
 * behaviours. The animal model is headless, so all of its behaviour can be
 * verified directly.
 */
@DisplayName("Animal — friendly waypoint behaviour")
class AnimalTest {

    private static final float WORLD_W = 1280f;
    private static final float WORLD_H = 768f;

    @Test
    void registersAllSevenSpecies() {
        assertEquals(7, AnimalType.values().length);
        Set<String> ids = new HashSet<>();
        for (AnimalType type : AnimalType.values()) {
            assertTrue(ids.add(type.getId()), "duplicate id " + type.getId());
        }
    }

    @Test
    void everySpeciesHasValidBehaviourData() {
        for (AnimalType type : AnimalType.values()) {
            assertTrue(type.getNameKey().startsWith("animal."), type.getId() + " name key");
            assertNotNull(type.getBodyColor(), type.getId() + " colour");
            assertTrue(type.getSize() > 8f && type.getSize() < 30f, type.getId() + " size");
            assertTrue(type.getSpeed() > 10f && type.getSpeed() < 120f, type.getId() + " speed");
            assertTrue(type.getWanderWeight() > 0f, type.getId() + " wanders");
            assertTrue(type.getEatWeight() > 0f, type.getId() + " eats");
            assertTrue(type.getSleepWeight() > 0f, type.getId() + " sleeps");
            assertTrue(type.getTotalWeight() > 0f, type.getId() + " total weight");
        }
    }

    @Test
    void byIdResolvesEverySpecies() {
        for (AnimalType type : AnimalType.values()) {
            assertSame(type, AnimalType.byId(type.getId()));
        }
        assertNull(AnimalType.byId("dragon"));
    }

    @Test
    void newAnimalStartsWithinBounds() {
        Animal animal = new Animal(AnimalType.DOG, WORLD_W / 2, WORLD_H / 2, 42L);
        assertTrue(animal.getX() >= 0f && animal.getX() <= WORLD_W);
        assertTrue(animal.getY() >= 0f && animal.getY() <= WORLD_H);
        assertNotNull(animal.getState());
        assertEquals(AnimalType.DOG, animal.getType());
        assertEquals(Animal.State.IDLE, animal.getState(), "fresh animal starts idle");
    }

    @Test
    void staysInsideWorldAfterLongSimulation() {
        Animal animal = new Animal(AnimalType.CAT, WORLD_W / 2, WORLD_H / 2, 7L);
        for (int i = 0; i < 60 * 60; i++) {
            animal.update(1f / 60f, WORLD_W, WORLD_H);
        }
        assertTrue(animal.getX() >= 0f && animal.getX() <= WORLD_W, "x in bounds: " + animal.getX());
        assertTrue(animal.getY() >= 0f && animal.getY() <= WORLD_H, "y in bounds: " + animal.getY());
    }

    @Test
    void wanderMovesTowardTheWaypoint() {
        Animal animal = new Animal(AnimalType.RABBIT, 200f, 200f, 11L);
        animal.startWanderTo(900f, 600f);
        assertEquals(Animal.State.WANDER, animal.getState());
        float before = (float) Math.hypot(900f - animal.getX(), 600f - animal.getY());
        animal.update(1f, WORLD_W, WORLD_H);
        float after = (float) Math.hypot(900f - animal.getX(), 600f - animal.getY());
        assertTrue(after < before, "moved closer to the waypoint: " + before + " -> " + after);
        assertTrue(Math.abs(after - before) <= AnimalType.RABBIT.getSpeed(),
            "moved no faster than the species' speed");
    }

    @Test
    void arrivalReachesTheWaypoint() {
        Animal animal = new Animal(AnimalType.SHEEP, 300f, 300f, 3L);
        animal.startWanderTo(320f, 310f);
        boolean reached = false;
        for (int i = 0; i < 120 && !reached; i++) {
            animal.update(1f / 60f, WORLD_W, WORLD_H);
            reached = Math.abs(animal.getX() - 320f) < 1f && Math.abs(animal.getY() - 310f) < 1f;
        }
        assertTrue(reached, "animal reached the waypoint: " + animal.getX() + "," + animal.getY());
    }

    @Test
    void idleEndsAndSomethingElseHappens() {
        Animal animal = new Animal(AnimalType.COW, 400f, 400f, 9L);
        assertEquals(Animal.State.IDLE, animal.getState());
        for (int i = 0; i < 60 * 8; i++) {
            animal.update(1f / 60f, WORLD_W, WORLD_H);
            if (animal.getState() != Animal.State.IDLE) break;
        }
        assertNotEquals(Animal.State.IDLE, animal.getState(), "idle always ends");
    }

    @Test
    void turnFinishesAndLeavesTheState() {
        Animal animal = new Animal(AnimalType.DOG, 100f, 100f, 5L);
        animal.forceTurnTo((float) Math.PI);
        assertEquals(Animal.State.TURN, animal.getState());
        for (int i = 0; i < 60 * 5; i++) {
            animal.update(1f / 60f, WORLD_W, WORLD_H);
            if (animal.getState() != Animal.State.TURN) break;
        }
        assertNotEquals(Animal.State.TURN, animal.getState(), "turn always completes");
    }

    @Test
    void allBehavioursAppearOverTime() {
        EnumSet<Animal.State> seen = EnumSet.noneOf(Animal.State.class);
        AnimalType[] species = AnimalType.values();
        for (int i = 0; i < 200; i++) {
            Animal animal = new Animal(species[i % species.length], WORLD_W / 2, WORLD_H / 2, i);
            for (int frame = 0; frame < 60 * 240; frame++) {
                animal.update(1f / 60f, WORLD_W, WORLD_H);
                seen.add(animal.getState());
            }
        }
        for (Animal.State state : Animal.State.values()) {
            assertTrue(seen.contains(state), "behaviour observed: " + state);
        }
    }

    @Test
    void sleepLastsLongerThanEating() {
        Animal animal = new Animal(AnimalType.SHEEP, 500f, 500f, 21L);
        // Feed the state machine until we catch both EAT and SLEEP and compare durations.
        Float eatDuration = null;
        Float sleepDuration = null;
        for (int i = 0; i < 2000 && (eatDuration == null || sleepDuration == null); i++) {
            animal.update(1f / 60f, WORLD_W, WORLD_H);
            if (animal.getState() == Animal.State.EAT && eatDuration == null) {
                eatDuration = animal.getStateTimer();
            } else if (animal.getState() == Animal.State.SLEEP && sleepDuration == null) {
                sleepDuration = animal.getStateTimer();
            }
        }
        assertNotNull(eatDuration, "saw an eat");
        assertNotNull(sleepDuration, "saw a sleep");
        assertTrue(sleepDuration > eatDuration, "sleep (" + sleepDuration + ") longer than eat (" + eatDuration + ")");
    }

    @Test
    void ambientPopulationIncludesEverySpecies() {
        AnimalSimulation simulation = new AnimalSimulation(WORLD_W, WORLD_H);
        simulation.spawnDefault("meadow");
        Set<String> seen = new HashSet<>();
        for (Animal animal : simulation.getAnimals()) {
            seen.add(animal.getType().getId());
        }
        assertTrue(simulation.getAnimals().size > 0, "population is not empty");
        for (AnimalType type : AnimalType.values()) {
            assertTrue(seen.contains(type.getId()), "population includes " + type.getId());
        }
    }

    @Test
    void populationIsDeterministicPerMap() {
        AnimalSimulation first = new AnimalSimulation(WORLD_W, WORLD_H);
        AnimalSimulation second = new AnimalSimulation(WORLD_W, WORLD_H);
        first.spawnDefault("volcano");
        second.spawnDefault("volcano");
        assertEquals(first.getAnimals().size, second.getAnimals().size);
        for (int i = 0; i < first.getAnimals().size; i++) {
            assertEquals(first.getAnimals().get(i).getX(), second.getAnimals().get(i).getX(), 0.001f);
            assertEquals(first.getAnimals().get(i).getY(), second.getAnimals().get(i).getY(), 0.001f);
            assertEquals(first.getAnimals().get(i).getType(), second.getAnimals().get(i).getType());
        }
    }

    @Test
    void animalsStillBehaveAfterLongSimulation() {
        AnimalSimulation simulation = new AnimalSimulation(WORLD_W, WORLD_H);
        simulation.spawnDefault("meadow");
        for (int i = 0; i < 60 * 60; i++) {
            simulation.update(1f / 60f);
        }
        for (Animal animal : simulation.getAnimals()) {
            assertTrue(animal.getX() >= 0f && animal.getX() <= WORLD_W, "x in bounds");
            assertTrue(animal.getY() >= 0f && animal.getY() <= WORLD_H, "y in bounds");
        }
    }
}
