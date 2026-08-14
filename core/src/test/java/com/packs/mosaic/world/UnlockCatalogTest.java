package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UnlockCatalog — the discovery and recipe registry")
class UnlockCatalogTest {

    @Test
    void registersAtLeastTwentyDiscoveries() {
        assertTrue(UnlockCatalog.getDiscoveries().size >= 20, "Task 1 requires at least 20 discoveries");
    }

    @Test
    void registersRecipes() {
        assertTrue(UnlockCatalog.getRecipes().size >= 6);
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            assertTrue(ids.add(unlock.getId()), "duplicate id: " + unlock.getId());
        }
    }

    @Test
    void lookupByUnknownIdReturnsNull() {
        assertNull(UnlockCatalog.get("does_not_exist"));
    }

    @Test
    void everyUnlockRewardsARegisteredBuilding() {
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            assertNotNull(BuildingCatalog.get(unlock.getRewardBuildingId()),
                unlock.getId() + " rewards unknown building " + unlock.getRewardBuildingId());
        }
    }

    @Test
    void everyRewardIsDiscoveryGatedNotStarGated() {
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            BuildingType reward = BuildingCatalog.get(unlock.getRewardBuildingId());
            assertTrue(reward.isDiscoveryReward(), unlock.getId() + " reward must not appear via stars");
        }
    }

    @Test
    void everyUnlockHasAtLeastOneRequirement() {
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            assertFalse(unlock.getRequirements().size == 0, unlock.getId() + " has no requirements");
        }
    }

    @Test
    void everyRequirementReferencesRegisteredTypes() {
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            for (UnlockRequirement requirement : unlock.getRequirements()) {
                for (String typeId : referencedTypeIds(requirement)) {
                    assertNotNull(BuildingCatalog.get(typeId),
                        unlock.getId() + " references unknown building " + typeId);
                }
            }
        }
    }

    @Test
    void recipeIngredientsAreAltogetherRequired() {
        UnlockDefinition cozy = UnlockCatalog.get("recipe_cozy_cottage");
        assertEquals(3, cozy.getRequirements().size);
    }

    private static Array<String> referencedTypeIds(UnlockRequirement requirement) {
        Array<String> ids = new Array<>();
        if (requirement instanceof CountRequirement) {
            ids.add(((CountRequirement) requirement).getTypeId());
        } else if (requirement instanceof SumCountRequirement) {
            ids.addAll(((SumCountRequirement) requirement).getTypeIds());
        } else if (requirement instanceof PresenceRequirement) {
            ids.addAll(((PresenceRequirement) requirement).getTypeIds());
        } else if (requirement instanceof AnyPresenceRequirement) {
            ids.addAll(((AnyPresenceRequirement) requirement).getTypeIds());
        }
        return ids;
    }
}
