package com.packs.mosaic.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Locale;

/**
 * Central hub for sound effects. Placeholder WAVs are synthesized
 * procedurally on first use and cached in local storage, so they only
 * need to be generated once and can be reused across runs.
 *
 * Playback is null-guarded against Gdx.audio being unavailable (headless
 * tests, missing audio backend), so merely loading this class is safe in
 * any environment. Volume lives here and is mirrored in GameSettings.
 */
public final class AudioManager {

    /** The distinct UI sounds the game can produce. */
    public enum Sfx {
        CLICK, PLACE, DELETE, STAR, CHALLENGE_COMPLETE, DISCOVERY
    }

    private static AudioManager instance;

    private final ObjectMap<Sfx, Sound> sounds = new ObjectMap<>();
    private float sfxVolume = 1f;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    /** Loads (generating on demand) every sound. Safe to call more than once. */
    public void init() {
        if (Gdx.audio == null) {
            return;
        }
        for (Sfx sfx : Sfx.values()) {
            if (!sounds.containsKey(sfx)) {
                sounds.put(sfx, loadSfx(sfx));
            }
        }
    }

    private Sound loadSfx(Sfx sfx) {
        FileHandle file = Gdx.files.local("audio/fx/" + sfx.name().toLowerCase(Locale.ROOT) + ".wav");
        if (!file.exists()) {
            file.parent().mkdirs();
            file.writeBytes(wavFor(sfx), false);
        }
        return Gdx.audio.newSound(file);
    }

    private byte[] wavFor(Sfx sfx) {
        switch (sfx) {
            case CLICK:              return WavGenerator.tone(700, 0.06f, 0.5f);
            case PLACE:              return WavGenerator.tone(440, 0.14f, 0.6f);
            case DELETE:             return WavGenerator.tone(180, 0.16f, 0.6f);
            case STAR:               return WavGenerator.sequence(new double[]{880, 1174.66}, 0.09, 0.5f);
            case CHALLENGE_COMPLETE: return WavGenerator.sequence(new double[]{523.25, 659.25, 783.99}, 0.12, 0.5f);
            case DISCOVERY:          return WavGenerator.sequence(new double[]{659.25, 880, 1046.5, 1318.51}, 0.11, 0.5f);
            default:                 return WavGenerator.tone(440, 0.1f, 0.5f);
        }
    }

    /** Plays a sound at the current SFX volume. No-op when audio is unavailable. */
    public void play(Sfx sfx) {
        if (Gdx.audio == null) {
            return;
        }
        Sound sound = sounds.get(sfx);
        if (sound == null) {
            init();
            sound = sounds.get(sfx);
        }
        if (sound != null) {
            sound.play(sfxVolume);
        }
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        instance = null;
    }
}
