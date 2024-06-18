package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PointServiceTest {

    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    public void setUp() {
        // Mock 객체 초기화
        MockitoAnnotations.openMocks(this);
        // PointService 객체 생성, Mock된 UserPointTable과 PointHistoryTable을 주입
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    public void chargePoint_NegativeAmount_ThrowsException() {
        // 음수 금액으로 포인트 충전 시 예외가 발생하는지 테스트
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.updatePointAndHistory(1L, -100L, TransactionType.CHARGE);
        });
    }

    @Test
    public void usePoint_NegativeAmount_ThrowsException() {
        // 음수 금액으로 포인트 사용 시 예외가 발생하는지 테스트
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.updatePointAndHistory(1L, -100L, TransactionType.USE);
        });
    }

    @Test
    public void testGetUserPoint_ExistingId() {
        // 기존 ID로 유저 포인트 조회 테스트
        long id = 1L;
        UserPoint userPoint = UserPoint.of(id, 100L);
        when(userPointTable.selectById(id)).thenReturn(userPoint);

        UserPoint result = pointService.getUserPoint(id);

        assertEquals(userPoint, result);
    }

    @Test
    public void testGetUserPoint_NonExistingId() {
        // 존재하지 않는 ID로 유저 포인트 조회 시 초기 포인트가 0인지 테스트
        long id = 1L;
        when(userPointTable.selectById(id)).thenReturn(UserPoint.of(id, 0L));

        UserPoint result = pointService.getUserPoint(id);

        assertEquals(0, result.point());
    }

    @Test
    public void testUpdatePointAndHistory_Charge() {
        // 포인트 충전 시 포인트 업데이트와 히스토리 기록 테스트
        long id = 1L;
        long amount = 100L;
        UserPoint userPoint = UserPoint.of(id, 0L);
        when(userPointTable.selectById(id)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(eq(id), anyLong())).thenAnswer(invocation -> UserPoint.of(id, (Long) invocation.getArguments()[1]));

        UserPoint result = pointService.updatePointAndHistory(id, amount, TransactionType.CHARGE);

        assertEquals(amount, result.point());
    }

    @Test
    public void testUpdatePointAndHistory_Use() {
        // 포인트 사용 시 포인트 업데이트와 히스토리 기록 테스트
        long id = 1L;
        long amount = 50L;
        UserPoint userPoint = UserPoint.of(id, 100L);
        when(userPointTable.selectById(id)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(eq(id), anyLong())).thenAnswer(invocation -> UserPoint.of(id, (Long) invocation.getArguments()[1]));

        UserPoint result = pointService.updatePointAndHistory(id, amount, TransactionType.USE);

        assertEquals(100L - amount, result.point());
    }

    @Test
    public void testUpdatePointAndHistory_InvalidTransactionType() {
        // 유효하지 않은 트랜잭션 타입으로 포인트 업데이트 시 예외가 발생하는지 테스트
        long id = 1L;
        long amount = 100L;

        assertThrows(IllegalArgumentException.class, () -> pointService.updatePointAndHistory(id, amount, null));
    }

    @Test
    public void testFindPointHistoriesByUserId() {
        // 특정 유저의 포인트 히스토리 조회 테스트
        long userId = 1L;
        PointHistory pointHistory = PointHistory.of(userId, 100L, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.singletonList(pointHistory));

        var result = pointService.findPointHistoriesByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(pointHistory, result.get(0));
    }

    @Test
    public void testConcurrentUpdatePointAndHistory() throws InterruptedException, ExecutionException {
        // 동시성 문제를 테스트하기 위해 포인트 충전 및 사용을 동시에 실행
        long id = 1L;
        UserPoint initialUserPoint = UserPoint.of(id, 0L);
        when(userPointTable.selectById(id)).thenReturn(initialUserPoint);
        when(userPointTable.insertOrUpdate(eq(id), anyLong())).thenAnswer(invocation -> UserPoint.of(id, (Long) invocation.getArguments()[1]));

        // 충전 작업과 사용 작업을 동시에 실행할 Callable 객체 생성
        Callable<UserPoint> chargeTask = () -> pointService.updatePointAndHistory(id, 100L, TransactionType.CHARGE);
        Callable<UserPoint> useTask = () -> pointService.updatePointAndHistory(id, 50L, TransactionType.USE);

        // 두 개의 스레드 풀을 사용하여 동시에 실행
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<UserPoint> chargeFuture = executorService.submit(chargeTask);
        Future<UserPoint> useFuture = executorService.submit(useTask);

        // 두 작업의 결과를 가져옴
        UserPoint chargeResult = chargeFuture.get();
        UserPoint useResult = useFuture.get();

        // 동시성 테스트의 결과를 검증
        long finalPoint = chargeResult.point() + useResult.point();
        assertTrue(finalPoint >= 0);

        // 스레드 풀 종료
        executorService.shutdown();
    }
}
