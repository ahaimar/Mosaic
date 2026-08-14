package com.packs.mosaic.audio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WavGenerator")
class WavGeneratorTest {

    @Test
    void headerIsAValidRiffWaveContainer() {
        byte[] wav = WavGenerator.tone(440, 0.1f, 0.5f);

        assertArrayEquals(new byte[]{'R', 'I', 'F', 'F'}, slice(wav, 0, 4));
        assertEquals(36 + (wav.length - 44),
            ByteBuffer.wrap(wav, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        assertArrayEquals(new byte[]{'W', 'A', 'V', 'E'}, slice(wav, 8, 4));
        assertArrayEquals(new byte[]{'f', 'm', 't', ' '}, slice(wav, 12, 4));
        assertEquals(16, ByteBuffer.wrap(wav, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        assertEquals(1, ByteBuffer.wrap(wav, 20, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());  // PCM
        assertEquals(1, ByteBuffer.wrap(wav, 22, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());  // mono
        assertEquals(WavGenerator.SAMPLE_RATE,
            ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        assertEquals(16, ByteBuffer.wrap(wav, 34, 2).order(ByteOrder.LITTLE_ENDIAN).getShort()); // bits
        assertArrayEquals(new byte[]{'d', 'a', 't', 'a'}, slice(wav, 36, 4));
        assertEquals(wav.length - 44,
            ByteBuffer.wrap(wav, 40, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    @Test
    void toneProducesTheExpectedNumberOfSamples() {
        byte[] wav = WavGenerator.tone(440, 0.1f, 0.5f);
        int samples = (wav.length - 44) / 2;
        assertEquals((int) (0.1f * WavGenerator.SAMPLE_RATE), samples);
    }

    @Test
    void toneIsAudibleNotSilent() {
        byte[] wav = WavGenerator.tone(440, 0.2f, 0.5f);
        int peak = 0;
        for (int i = 44; i < wav.length; i += 2) {
            short sample = ByteBuffer.wrap(wav, i, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            peak = Math.max(peak, Math.abs(sample));
        }
        assertTrue(peak > Short.MAX_VALUE / 3, "expected an audible tone, peak=" + peak);
    }

    @Test
    void sequenceLengthMatchesTheNumberOfNotes() {
        byte[] wav = WavGenerator.sequence(new double[]{523.25, 659.25, 783.99}, 0.12, 0.5f);
        int samples = (wav.length - 44) / 2;
        int noteSamples = (int) (0.12 * WavGenerator.SAMPLE_RATE);
        assertEquals(3 * noteSamples, samples);
    }

    private static byte[] slice(byte[] data, int from, int length) {
        byte[] out = new byte[length];
        System.arraycopy(data, from, out, 0, length);
        return out;
    }
}
