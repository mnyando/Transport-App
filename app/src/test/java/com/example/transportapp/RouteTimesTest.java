package com.example.transportapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RouteTimesTest {

    @Test
    public void format_padsAndUsesLatinDigits() {
        assertEquals("05:07", RouteTimes.format(5, 7));
        assertEquals("15:30", RouteTimes.format(15, 30));
    }

    @Test
    public void parse_readsStoredValue() {
        assertArrayEquals(new int[]{15, 45}, RouteTimes.parse("15:45"));
        assertArrayEquals(new int[]{7, 5}, RouteTimes.parse(" 7:05 "));
    }

    @Test
    public void parse_fallsBackToSevenForBadInput() {
        assertArrayEquals(new int[]{7, 0}, RouteTimes.parse(null));
        assertArrayEquals(new int[]{7, 0}, RouteTimes.parse(""));
        assertArrayEquals(new int[]{7, 0}, RouteTimes.parse("25:00"));
        assertArrayEquals(new int[]{7, 0}, RouteTimes.parse("noon"));
    }

    @Test
    public void dropOffAfterPickup() {
        assertTrue(RouteTimes.dropOffAfterPickup("07:30", "15:45"));
        assertFalse(RouteTimes.dropOffAfterPickup("15:45", "07:30"));
        assertFalse(RouteTimes.dropOffAfterPickup("07:30", "07:30"));
        assertFalse(RouteTimes.dropOffAfterPickup("", "07:30"));
    }
}
