package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PointHistoryTableTest {

    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    public void setUp() {
        pointHistoryTable = new PointHistoryTable();
    }

    @Test
    public void testSelectAllByUserId_MultipleEntries() {
        long userId = 1L;
        int numEntries = 5;

        for (int i = 0; i < numEntries; i++) {
            pointHistoryTable.insert(userId, i, TransactionType.CHARGE, System.currentTimeMillis());
        }

        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

        assertEquals(numEntries, pointHistories.size());
        pointHistories.forEach(pointHistory -> assertEquals(userId, pointHistory.userId()));
    }

    @Test
    public void testSelectAllByUserId_NoEntries() {
        long userId = 1L;

        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

        assertTrue(pointHistories.isEmpty());
    }
}