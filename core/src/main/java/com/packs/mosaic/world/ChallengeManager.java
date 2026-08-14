package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Drives the active challenge: holds the current ChallengeDefinition,
 * counts placed buildings on the VillageGrid, awards stars via
 * PlayerProgress when the requirements are met, and advances to the next
 * challenge (completing several in a row is possible if the grid already
 * satisfies them). When the last challenge is completed, the active
 * challenge becomes null — the game has no more objectives.
 *
 * Call {@link #checkCompletion()} after any grid mutation (placement,
 * deletion, undo, redo). UI observes progress through ChallengeListener.
 */
public class ChallengeManager {

    /** Receives challenge lifecycle events (toasts, panels, unlock refreshes). */
    public interface ChallengeListener {
        /** The active challenge changed (including to null when all are done). */
        default void onChallengeChanged(ChallengeDefinition challenge) {}
        /** The active challenge was completed and its stars awarded. */
        default void onChallengeCompleted(ChallengeDefinition challenge) {}
        /** Counts changed but the challenge is not complete yet. */
        default void onChallengeProgress(ChallengeDefinition challenge) {}
    }

    private final VillageGrid grid;
    private final PlayerProgress progress;
    private final Array<ChallengeListener> listeners = new Array<>();
    private int currentIndex = -1;

    /** Starts on the first catalog challenge. */
    public ChallengeManager(VillageGrid grid, PlayerProgress progress) {
        this(grid, progress, ChallengeCatalog.getFirst() == null ? null : ChallengeCatalog.getFirst().getId());
    }

    /** Starts on the named challenge; unknown ids fall back to the first challenge. */
    public ChallengeManager(VillageGrid grid, PlayerProgress progress, String startingChallengeId) {
        this.grid = grid;
        this.progress = progress;
        if (startingChallengeId != null) {
            setChallenge(startingChallengeId);
        }
    }

    public void addListener(ChallengeListener listener) {
        listeners.add(listener);
    }

    /** Jumps to a specific challenge by id (unknown ids restart from the first). */
    public void setChallenge(String challengeId) {
        ChallengeDefinition definition = ChallengeCatalog.get(challengeId);
        if (definition == null) definition = ChallengeCatalog.getFirst();
        currentIndex = definition == null ? -1 : ChallengeCatalog.getAll().indexOf(definition, true);
        notifyChanged();
    }

    public ChallengeDefinition getCurrentChallenge() {
        if (currentIndex < 0 || currentIndex >= ChallengeCatalog.getAll().size) return null;
        return ChallengeCatalog.getAll().get(currentIndex);
    }

    /** Placed counts for every type currently on the grid (multi-cell buildings counted once). */
    public ObjectMap<String, Integer> getCurrentCounts() {
        return GridCounts.count(grid);
    }

    public boolean isCurrentCompleted() {
        ChallengeDefinition challenge = getCurrentChallenge();
        return challenge != null && challenge.isSatisfiedBy(getCurrentCounts());
    }

    /**
     * Re-reads the grid; if the active challenge's requirements are met,
     * awards the stars, advances, and re-checks (the next challenge may
     * already be satisfied). Otherwise notifies listeners of the new
     * counts so progress UI can refresh.
     */
    public void checkCompletion() {
        ChallengeDefinition challenge = getCurrentChallenge();
        if (challenge == null) return;

        if (challenge.isSatisfiedBy(getCurrentCounts())) {
            progress.addStars(challenge.getStarReward());
            currentIndex++;
            for (ChallengeListener l : listeners) l.onChallengeCompleted(challenge);
            notifyChanged();
            checkCompletion(); // the newly active challenge may also already be complete
        } else {
            for (ChallengeListener l : listeners) l.onChallengeProgress(challenge);
        }
    }

    private void notifyChanged() {
        ChallengeDefinition challenge = getCurrentChallenge();
        for (ChallengeListener l : listeners) l.onChallengeChanged(challenge);
    }
}
