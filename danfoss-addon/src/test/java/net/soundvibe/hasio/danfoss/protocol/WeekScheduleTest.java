package net.soundvibe.hasio.danfoss.protocol;

import net.soundvibe.hasio.danfoss.protocol.config.Dominion;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WeekScheduleTest {

    @Test
    void home_day_packet_matches_the_app() {
        var pkt = new Dominion.Packet(103, 4109, WeekSchedule.day(false));
        assertEquals("0c000000000000000000000000", pkt.payloadHex());
    }

    @Test
    void away_day_packet_matches_the_app() {
        var pkt = new Dominion.Packet(103, 4109, WeekSchedule.day(true));
        assertEquals("0caaaaaaaaaaaaaaaaaaaaaaaa", pkt.payloadHex());
    }
}
