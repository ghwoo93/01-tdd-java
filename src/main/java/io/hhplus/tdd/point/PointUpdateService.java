package io.hhplus.tdd.point;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@Service
public class PointUpdateService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointUpdateService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public long calculateNewAmount(long currentAmount, long amount, TransactionType type) {
        switch (type) {
            case CHARGE:
                return currentAmount + amount;
            case USE:
                if (currentAmount < amount) {
                    throw new IllegalArgumentException("Insufficient balance");
                }
                return currentAmount - amount;
            default:
                throw new IllegalArgumentException("Unsupported transaction type");
        }
    }

    public UserPoint updateUserPoint(long userId, long newAmount) {
        UserPoint userPoint = userPointTable.insertOrUpdate(userId, newAmount);
        if (userPoint == null) {
            throw new IllegalStateException("Failed to update user point");
        }
        return userPoint;
    }

    public PointHistory updatePointHistory(Long userId, Long amount, TransactionType type) {
        PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, System.currentTimeMillis());
        if (pointHistory == null) {
            throw new IllegalStateException("Failed to update point history");
        }
        return pointHistory;
    }
}