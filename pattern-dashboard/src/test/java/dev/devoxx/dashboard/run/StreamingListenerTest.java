package dev.devoxx.dashboard.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/** What the Scope tab shows for a value, which is a variables table rather than a wall of text. */
class StreamingListenerTest {

    @Test
    void scopeEntriesCarryAReadableTypeAndSize() {
        var text = StreamingListener.describe("hello");
        assertEquals("String", text.type());
        assertEquals(5, (int) text.size());

        // List.of(...) is really an ImmutableCollections$ListN; the variables table must not
        // leak that at the audience.
        var list = StreamingListener.describe(List.of("a", "b", "c"));
        assertEquals("List", list.type());
        assertEquals(3, (int) list.size());

        // Sizeless scalars simply have no size, rather than a misleading 0.
        var number = StreamingListener.describe(42);
        assertEquals("Integer", number.type());
        assertNull(number.size());

        assertEquals("null", StreamingListener.describe(null).type());
    }
}
