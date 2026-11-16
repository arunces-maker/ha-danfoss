package net.soundvibe.hasio.danfoss.protocol;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ChangeJournalTest {

    @Test
    void first_value_is_remembered_silently() {
        var sut = new ChangeJournal();
        assertTrue(sut.record(190, 29252, "0e01").isEmpty());
    }

    @Test
    void same_value_is_not_reported() {
        var sut = new ChangeJournal();
        sut.record(190, 29252, "0e01");
        assertTrue(sut.record(190, 29252, "0e01").isEmpty());
    }

    @Test
    void changed_value_is_reported_with_old_and_new() {
        var sut = new ChangeJournal();
        sut.record(190, 29252, "0e01");
        var line = sut.record(190, 29252, "0e01ff");
        assertEquals("controller change: all-rooms code=29252 0e01 -> 0e01ff", line.orElseThrow());
    }

    @Test
    void room_number_is_class_minus_99() {
        var sut = new ChangeJournal();
        sut.record(102, 4107, "00");
        var line = sut.record(102, 4107, "14");
        assertEquals("controller change: room=3 code=4107 00 -> 14", line.orElseThrow());
    }

    @Test
    void noisy_codes_and_uninteresting_classes_are_ignored() {
        var sut = new ChangeJournal();
        sut.record(101, 768, "9808");
        assertTrue(sut.record(101, 768, "9908").isEmpty()); // temperature
        sut.record(5, 29237, "01");
        assertTrue(sut.record(5, 29237, "02").isEmpty());   // clock
        sut.record(60, 4104, "01");
        assertTrue(sut.record(60, 4104, "02").isEmpty());   // outputs
    }
}
