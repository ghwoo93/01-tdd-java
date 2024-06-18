package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import jakarta.transaction.Transactional;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    private UserPoint updateUserPoint(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

    public List<PointHistory> findPointHistoriesByUserId(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    private PointHistory updatePointHistory(Long userId, Long amount, TransactionType type) {
        return pointHistoryTable.insert(userId, amount, type, System.currentTimeMillis());
    }

    @Transactional
    public synchronized UserPoint updatePointAndHistory(long id, long amount, TransactionType type) {
        // 포인트 충전
        UserPoint currentPoint = userPointTable.selectById(id);
        long newAmount;
        
        if (type == TransactionType.CHARGE) {
            newAmount = currentPoint.point() + amount;
        } else if (type == TransactionType.USE) {
            // 잔고 검증
            if (currentPoint.point() < amount) {
                throw new IllegalArgumentException("Insufficient balance");
            }
            newAmount = currentPoint.point() - amount;
        } else {
            throw new IllegalArgumentException("Invalid transaction type");
        }

        // 포인트 업데이트 및 히스토리 업데이트
        UserPoint updatedUserPoint = updateUserPoint(id, newAmount);
        updatePointHistory(id, amount, type);

        return updatedUserPoint;
    }
}
