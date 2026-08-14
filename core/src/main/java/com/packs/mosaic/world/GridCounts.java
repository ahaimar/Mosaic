package com.packs.mosaic.world;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Computes per-type placed counts for a VillageGrid. Shared by
 * ChallengeManager (challenge requirements) and DiscoveryManager (unlock
 * requirements) so both read the grid identically. Multi-cell buildings
 * are counted once, at their origin cell.
 */
public final class GridCounts {

    private GridCounts() {
    }

    public static ObjectMap<String, Integer> count(VillageGrid grid) {
        ObjectMap<String, Integer> counts = new ObjectMap<>();
        for (int col = 0; col < grid.getCols(); col++) {
            for (int row = 0; row < grid.getRows(); row++) {
                VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
                if (occupant instanceof BuildingObject) {
                    BuildingObject object = (BuildingObject) occupant;
                    if (object.getOriginCol() == col && object.getOriginRow() == row) {
                        String typeId = object.getType().getId();
                        counts.put(typeId, counts.get(typeId, 0) + 1);
                    }
                }
            }
        }
        return counts;
    }
}
