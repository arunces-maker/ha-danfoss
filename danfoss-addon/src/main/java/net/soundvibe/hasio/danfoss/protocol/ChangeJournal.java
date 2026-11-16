package net.soundvibe.hasio.danfoss.protocol;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the last payload of every controller parameter and reports only CHANGES.
 * <p>
 * Used to log, at info level and permanently, what the controller changes by itself (or when the Danfoss app
 * changes something), without logging the constant stream of timestamps, temperatures and valve duty cycles.
 * The first value seen for a parameter (the dump the controller sends on connect) is remembered silently.
 */
final class ChangeJournal {

    /** Rails (51-53), rooms (99-143) and the "all rooms" class (190). Outputs, repeaters and system classes are skipped. */
    private static boolean interestingClass(int msgClass) {
        return (msgClass >= 51 && msgClass <= 53) || (msgClass >= 99 && msgClass <= 143) || msgClass == 190;
    }

    /** Parameters that change all the time and say nothing about modes/plans: timestamps, temperatures, valve state. */
    private static final Set<Integer> NOISY_CODES = Set.of(
            4,      // timestamp
            304,    // time
            768,    // room temperature
            772,    // floor temperature
            780,    // valve duty cycle
            809,    // displayed temperature
            816,    // temperature
            4115,   // heating state (already exposed as the `mode` attribute)
            4144, 4145, 4146, // output duty cycles
            4608,   // output state
            28739   // LED indication
    );

    private final Map<String, String> last = new ConcurrentHashMap<>();

    /**
     * @return a log line describing the change, or empty if nothing worth logging happened
     */
    Optional<String> record(int msgClass, int msgCode, String payloadHex) {
        if (!interestingClass(msgClass) || NOISY_CODES.contains(msgCode)) {
            return Optional.empty();
        }
        var key = msgClass + ":" + msgCode;
        var previous = last.put(key, payloadHex);
        if (previous == null || previous.equals(payloadHex)) {
            return Optional.empty();
        }
        var who = (msgClass >= 99 && msgClass <= 143) ? "room=" + (msgClass - 99)
                : msgClass == 190 ? "all-rooms" : "rail=" + msgClass;
        return Optional.of("controller change: " + who + " code=" + msgCode + " " + previous + " -> " + payloadHex);
    }
}
