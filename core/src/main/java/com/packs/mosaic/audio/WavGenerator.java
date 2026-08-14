package com.packs.mosaic.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Synthesizes tiny placeholder WAV files (RIFF/WAVE, 16-bit mono PCM,
 * 44100 Hz) entirely in code, so the game ships without external audio
 * assets. Pure Java — no libGDX dependency — so it is trivially testable.
 *
 * Every sample passes through a short attack / longer release envelope
 * so tones start and stop without audible clicks.
 */
public final class WavGenerator {

    public static final int SAMPLE_RATE = 44100;

    private WavGenerator() {
    }

    /** A single sine tone of the given frequency (Hz) and length (seconds). */
    public static byte[] tone(double frequency, double seconds, float volume) {
        int length = Math.max(1, (int) (seconds * SAMPLE_RATE));
        double[] samples = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / SAMPLE_RATE;
            samples[i] = Math.sin(2 * Math.PI * frequency * t) * envelope(i, length);
        }
        return toWav(samples, volume);
    }

    /** A short melody: each frequency plays for noteSeconds, back to back. */
    public static byte[] sequence(double[] frequencies, double noteSeconds, float volume) {
        int noteLength = Math.max(1, (int) (noteSeconds * SAMPLE_RATE));
        int length = noteLength * frequencies.length;
        double[] samples = new double[length];
        for (int n = 0; n < frequencies.length; n++) {
            for (int i = 0; i < noteLength; i++) {
                double t = (double) i / SAMPLE_RATE;
                samples[n * noteLength + i] =
                    Math.sin(2 * Math.PI * frequencies[n] * t) * envelope(i, noteLength);
            }
        }
        return toWav(samples, volume);
    }

    /**
     * Linear attack (30ms) then release over the final 40% of the tone.
     * Shorter than ~60ms notes just get a symmetric attack/release ramp.
     */
    private static double envelope(int i, int length) {
        int attack = Math.min(length / 2, Math.max(1, (int) (0.03 * SAMPLE_RATE)));
        int releaseStart = Math.max(attack, (int) (length * 0.6));
        if (i < attack) {
            return (double) i / attack;
        }
        if (i >= releaseStart) {
            int releaseLength = Math.max(1, length - releaseStart);
            return 1.0 - (double) (i - releaseStart) / releaseLength;
        }
        return 1.0;
    }

    /** Packs double samples (scaled by volume) into a complete WAV byte array. */
    private static byte[] toWav(double[] samples, float volume) {
        int dataSize = samples.length * 2;
        ByteBuffer out = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        out.put(new byte[]{'R', 'I', 'F', 'F'});
        out.putInt(36 + dataSize);
        out.put(new byte[]{'W', 'A', 'V', 'E'});
        out.put(new byte[]{'f', 'm', 't', ' '});
        out.putInt(16);               // fmt chunk size
        out.putShort((short) 1);      // PCM
        out.putShort((short) 1);      // mono
        out.putInt(SAMPLE_RATE);
        out.putInt(SAMPLE_RATE * 2);  // byte rate
        out.putShort((short) 2);      // block align
        out.putShort((short) 16);     // bits per sample
        out.put(new byte[]{'d', 'a', 't', 'a'});
        out.putInt(dataSize);

        for (double sample : samples) {
            double clamped = Math.max(-1.0, Math.min(1.0, sample * volume));
            out.putShort((short) (clamped * Short.MAX_VALUE));
        }
        return out.array();
    }
}
