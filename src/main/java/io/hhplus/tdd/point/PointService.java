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
        validateId(id);
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new IllegalArgumentException("User not found");
        }
        return userPoint;
    }

    private UserPoint updateUserPoint(long id, long amount) {
        validateId(id);
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        UserPoint userPoint = userPointTable.insertOrUpdate(id, amount);
        if (userPoint == null) {
            throw new IllegalStateException("Failed to update user point");
        }
        return userPoint;
    }

    public List<PointHistory> findPointHistoriesByUserId(Long userId) {
        validateId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    private PointHistory updatePointHistory(Long userId, Long amount, TransactionType type) {
        validateId(userId);
        validateAmount(amount);
        validateTransactionType(type);
        PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, System.currentTimeMillis());
        if (pointHistory == null) {
            throw new IllegalStateException("Failed to update point history");
        }
        return pointHistory;
    }

    @Transactional
    public synchronized UserPoint updatePointAndHistory(long userId, long amount, TransactionType type) throws IllegalArgumentException{
        
        // 포인트 충전 전 검증
        validateId(userId);
        validateAmount(amount);
        validateTransactionType(type);
        if (!(type instanceof TransactionType)) 
            throw new IllegalArgumentException("Type must be an instance of TransactionType");
        
        UserPoint currentPoint = userPointTable.selectById(userId);
        if (currentPoint == null) throw new IllegalArgumentException("User not found");
        
        long newAmount;

        // 잔고 검증
        if (type == TransactionType.CHARGE && amount <= 0) 
            throw new IllegalArgumentException("Invalid amount");
        
        switch (type) {
            case CHARGE:
                newAmount = currentPoint.point() + amount;
                break;
            case USE:
                if (currentPoint.point() < amount) {
                    throw new IllegalArgumentException("Insufficient balance");
                }
                newAmount = currentPoint.point() - amount;
                break;
            default:
                throw new IllegalArgumentException("Unsupported transaction type");
        }

        // 포인트 업데이트 및 히스토리 업데이트
        UserPoint updatedUserPoint = updateUserPoint(userId, newAmount);
        updatePointHistory(userId, amount, type);

        return updatedUserPoint;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid ID");
        }
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    private void validateTransactionType(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Invalid transaction type");
        }
    }
}
