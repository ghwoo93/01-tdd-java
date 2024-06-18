package io.hhplus.tdd.database;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserPointTableTest {

    private UserPointTable userPointTable;

    @BeforeEach
    public void setUp() {
        userPointTable = new UserPointTable();
    }

    @Test
    public void testSelectById_ExistingId() {
        long id = 1L;
        long point = 100L;
        userPointTable.insertOrUpdate(id, point);

        UserPoint userPoint = userPointTable.selectById(id);

        assertNotNull(userPoint);
        assertEquals(id, userPoint.id());
        assertEquals(point, userPoint.point());
    }

    @Test
    public void testSelectById_NonExistingId() {
        long id = 1L;

        UserPoint userPoint = userPointTable.selectById(id);

        assertNotNull(userPoint);
        assertEquals(id, userPoint.id());
        assertEquals(0, userPoint.point());
    }
}