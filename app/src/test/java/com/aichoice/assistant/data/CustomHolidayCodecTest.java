package com.aichoice.assistant.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomHolidayCodecTest {

    @Test
    public void roundTripsAndSortsByDate() {
        Map<LocalDate, String> source = new LinkedHashMap<>();
        source.put(LocalDate.of(2027, 10, 8), "公司假期");
        source.put(LocalDate.of(2026, 2, 17), "春节调休");

        String encoded = CustomHolidayCodec.encode(source);
        Map<LocalDate, String> decoded = CustomHolidayCodec.decode(encoded);

        assertEquals(source, decoded);
        assertTrue(encoded.indexOf("2026-02-17") < encoded.indexOf("2027-10-08"));
    }

    @Test
    public void blankAndNullInputsProduceAnEmptyList() {
        assertEquals("[]", CustomHolidayCodec.encode(null));
        assertTrue(CustomHolidayCodec.decode(null).isEmpty());
        assertTrue(CustomHolidayCodec.decode("").isEmpty());
        assertTrue(CustomHolidayCodec.decode("not-json").isEmpty());
    }

    @Test
    public void skipsMalformedEntriesButKeepsValidOnes() {
        String raw = "["
                + "{\"date\":\"not-a-date\",\"name\":\"bad\"},"
                + "{\"name\":\"missing date\"},"
                + "{\"date\":\"2026-01-01\",\"name\":\"元旦\"}"
                + "]";

        Map<LocalDate, String> decoded = CustomHolidayCodec.decode(raw);

        assertEquals(1, decoded.size());
        assertEquals("元旦", decoded.get(LocalDate.of(2026, 1, 1)));
    }
}
