package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ChallengeCatalog — starter challenge registry")
class ChallengeCatalogTest {

    @Test
    void registersThreeStarterChallenges() {
        assertEquals(3, ChallengeCatalog.getAll().size);
    }

    @Test
    void idsAreUnique() {
        Array<String> ids = new Array<>();
        for (ChallengeDefinition challenge : ChallengeCatalog.getAll()) {
            assertFalse(ids.contains(challenge.getId(), false), "duplicate id " + challenge.getId());
            ids.add(challenge.getId());
        }
    }

    @Test
    void everyChallengeHasPositiveRewardAndValidRequirements() {
        for (ChallengeDefinition challenge : ChallengeCatalog.getAll()) {
            assertTrue(challenge.getStarReward() > 0, challenge.getId() + " must reward stars");
            assertFalse(challenge.getRequiredCounts().size == 0, challenge.getId() + " must require something");
            for (ObjectMap.Entry<String, Integer> requirement : challenge.getRequiredCounts()) {
                assertTrue(requirement.value > 0, challenge.getId() + " count for " + requirement.key);
                assertNotNull(BuildingCatalog.get(requirement.key),
                    challenge.getId() + " references unknown type " + requirement.key);
            }
        }
    }

    @Test
    void getReturnsRegisteredDefinitions() {
        assertEquals("challenge_1", ChallengeCatalog.get("challenge_1").getId());
        assertNull(ChallengeCatalog.get("no_such_challenge"));
    }

    @Test
    void getFirstIsNotNull() {
        assertNotNull(ChallengeCatalog.getFirst());
    }

    @Test
    void getNextTraversesInOrderAndEndsNull() {
        ChallengeDefinition first = ChallengeCatalog.getFirst();
        ChallengeDefinition second = ChallengeCatalog.getNext(first);
        ChallengeDefinition third = ChallengeCatalog.getNext(second);
        assertEquals(1, ChallengeCatalog.getAll().indexOf(second, true));
        assertEquals(2, ChallengeCatalog.getAll().indexOf(third, true));
        assertNull(ChallengeCatalog.getNext(third));
    }

    @Test
    void firstChallengeOnlyNeedsFreeTypes() {
        ChallengeDefinition first = ChallengeCatalog.getFirst();
        for (ObjectMap.Entry<String, Integer> requirement : first.getRequiredCounts()) {
            assertEquals(0, BuildingCatalog.get(requirement.key).getStarsToUnlock(),
                first.getId() + " must be completable on a fresh save");
        }
    }
}
