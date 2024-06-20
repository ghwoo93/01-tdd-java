package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ValidationService validationService;
    private final PointUpdateService pointUpdateService;

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable, 
                        ValidationService validationService, PointUpdateService pointUpdateService) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.validationService = validationService;
        this.pointUpdateService = pointUpdateService;
    }

    public UserPoint getUserPoint(long id) {
        validationService.validateId(id);
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new IllegalArgumentException("User not found");
        }
        return userPoint;
    }

    public List<PointHistory> findPointHistoriesByUserId(Long userId) {
        validationService.validateId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    @Transactional
    public synchronized UserPoint updatePointAndHistory(long userId, long amount, TransactionType type) {
        validationService.validateId(userId);
        validationService.validateAmount(amount);
        validationService.validateTransactionType(type);

        UserPoint currentPoint = userPointTable.selectById(userId);
        if (currentPoint == null) {
            throw new IllegalArgumentException("User not found");
        }

        long newAmount = pointUpdateService.calculateNewAmount(currentPoint.point(), amount, type);
        UserPoint updatedUserPoint = pointUpdateService.updateUserPoint(userId, newAmount);
        pointUpdateService.updatePointHistory(userId, amount, type);

        //return userPointTable.selectById(userId);
        return updatedUserPoint;
    }
}
