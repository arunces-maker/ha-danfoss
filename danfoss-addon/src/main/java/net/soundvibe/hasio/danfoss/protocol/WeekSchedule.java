package net.soundvibe.hasio.danfoss.protocol;

import java.util.Arrays;

/**
 * Weekly schedule of one room, as the Danfoss app writes it (packets 4108..4114, Monday..Sunday).
 * The payload is 12 bytes (the packet adds the length byte 0x0c): 0x00 = Home all day, 0xaa = Away all day.
 * Observed 2026-11-17 in the app's debug log: an Away day is 0c + 12 x aa, a Home day is 0c + 12 x 00.
 */
final class WeekSchedule {

    static final int DAY_BYTES = 12;

    private WeekSchedule() {
    }

    static byte[] day(boolean away) {
        var bytes = new byte[DAY_BYTES];
        Arrays.fill(bytes, away ? (byte) 0xaa : (byte) 0x00);
        return bytes;
    }
}
