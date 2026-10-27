package com.safehome.integration.building;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FloorInferenceTest {
    private BuildingUnit unit(Integer floor, String room, BuildingQuery.Ground ground) {
        return new BuildingUnit("unit", "parent", "11530", "10700", "0", "0355", "0018",
                "synthetic", null, null, ground, floor, room, "synthetic");
    }
    private Resolution match(int floor, String room, BuildingUnit... units) {
        var query = new BuildingQuery("11530", "10700", "0", "0355", "0018", null,
                BuildingQuery.Ground.ABOVE_GROUND, floor, room);
        return new BuildingMatcher().match(query, List.of(units), true, Instant.now());
    }
    @Test void estimatesNumericRoomsAndPreservesTheMissingSourceFloor() {
        for (String room : List.of("202호", "제0202호", "１２０３호", "1015호")) {
            int floor = room.contains("202") ? 2 : room.startsWith("１") ? 12 : 10;
            var r = match(floor, room, unit(null, room, BuildingQuery.Ground.ABOVE_GROUND));
            assertEquals(Resolution.Status.MATCHED, r.status());
            assertEquals("FLOOR_INFERRED_FROM_UNIT_NUMBER", r.reason());
            assertNull(r.candidates().getFirst().floorNumber());
            assertEquals(room, r.candidates().getFirst().unitName());
        }
    }
    @Test void inferenceConflictIsUnverifiableAndExplicitFloorAlwaysWins() {
        assertEquals("FLOOR_INFERENCE_CONFLICT", match(3,"202",unit(null,"202",BuildingQuery.Ground.ABOVE_GROUND)).reason());
        var explicit=match(3,"202",unit(3,"202",BuildingQuery.Ground.ABOVE_GROUND));
        assertEquals(Resolution.Status.MATCHED,explicit.status()); assertNull(explicit.reason());
        assertEquals(Resolution.Status.NOT_FOUND,match(2,"202",unit(3,"202",BuildingQuery.Ground.ABOVE_GROUND)).status());
        assertEquals("FLOOR_INFERENCE_CONFLICT",match(2,"202",unit(null,"3층 202호",BuildingQuery.Ground.ABOVE_GROUND)).reason());
    }
    @Test void unsupportedRoomsGroundAndIncompleteDataNeverBecomeConfirmed() {
        for(String room:List.of("02","A202","B202","200","202-1"))
            assertEquals(Resolution.Status.UNVERIFIABLE,match(2,room,unit(null,room,BuildingQuery.Ground.ABOVE_GROUND)).status());
        assertEquals(Resolution.Status.UNVERIFIABLE,match(2,"202",unit(null,"202",null)).status());
        var query=new BuildingQuery("11530","10700","0","0355","0018",null,BuildingQuery.Ground.BELOW_GROUND,2,"202");
        assertEquals(Resolution.Status.UNVERIFIABLE,new BuildingMatcher().match(query,
                List.of(unit(null,"202",BuildingQuery.Ground.BELOW_GROUND)),true,Instant.now()).status());
        assertEquals(Resolution.Status.AMBIGUOUS,match(2,"202",unit(null,"202",BuildingQuery.Ground.ABOVE_GROUND),unit(null,"202",BuildingQuery.Ground.ABOVE_GROUND)).status());
        assertFalse(new BuildingMatcher().match(query,List.of(),false,Instant.now()).collectionComplete());
    }
}
