package com.packs.mosaic.audio;

import com.badlogic.gdx.Gdx;
import com.packs.mosaic.support.HeadlessGdx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("AudioManager — headless safety")
class AudioManagerTest {

    @BeforeAll
    static void beforeAll() {
        HeadlessGdx.install();
    }

    @AfterEach
    void tearDown() {
        AudioManager.getInstance().dispose();
    }

    @Test
    void initIsANoOpWithoutAnAudioBackend() {
        assertNull(Gdx.audio);
        AudioManager.getInstance().init();
        assertNull(Gdx.audio);
    }

    @Test
    void playIsANoOpWithoutAnAudioBackend() {
        AudioManager.getInstance().play(AudioManager.Sfx.CLICK);
        AudioManager.getInstance().play(AudioManager.Sfx.CHALLENGE_COMPLETE);
    }

    @Test
    void sfxVolumeIsClampedToTheUnitRange() {
        AudioManager.getInstance().setSfxVolume(5f);
        assertEquals(1f, AudioManager.getInstance().getSfxVolume());
        AudioManager.getInstance().setSfxVolume(-2f);
        assertEquals(0f, AudioManager.getInstance().getSfxVolume());
        AudioManager.getInstance().setSfxVolume(0.4f);
        assertEquals(0.4f, AudioManager.getInstance().getSfxVolume());
    }
}
