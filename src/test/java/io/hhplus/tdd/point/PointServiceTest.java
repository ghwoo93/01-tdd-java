package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PointServiceTest {
    
    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private ValidationService validationService;

    @Mock
    private PointUpdateService pointUpdateService;

    @BeforeEach
    public void setUp() {
        // MockitoAnnotations.openMocks(this) 호출을 통해 현재 클래스에 있는 @Mock 애노테이션으로 정의된 목 객체들을 초기화한다.
        MockitoAnnotations.openMocks(this);
    
        // ValidationService의 메서드들에 대한 목 행위를 정의한다.
        // ID 검증 메서드가 호출될 때 아무 동작도 하지 않도록 설정한다.
        doNothing().when(validationService).validateId(anyLong());
    
        // 금액 검증 메서드가 호출될 때 아무 동작도 하지 않도록 설정한다.
        doNothing().when(validationService).validateAmount(anyLong());
    
        // 트랜잭션 타입 검증 메서드가 호출될 때 아무 동작도 하지 않도록 설정한다.
        doNothing().when(validationService).validateTransactionType(any(TransactionType.class));
        
        // PointUpdateService의 calculateNewAmount 메서드에 대한 목 행위를 정의한다.
        // 메서드가 호출되었을 때 인자로 전달된 현재 금액, 변경할 금액, 트랜잭션 타입을 사용하여 새로운 금액을 계산한다.
        when(pointUpdateService.calculateNewAmount(anyLong(), anyLong(), any(TransactionType.class)))
                .thenAnswer(invocation -> {
                    // 현재 금액을 첫 번째 인자로 받음
                    long currentAmount = invocation.getArgument(0);
                    // 변경할 금액을 두 번째 인자로 받음
                    long amount = invocation.getArgument(1);
                    // 트랜잭션 타입을 세 번째 인자로 받음
                    TransactionType type = invocation.getArgument(2);
                    // 트랜잭션 타입에 따라 새로운 금액을 계산
                    return type == TransactionType.CHARGE ? currentAmount + amount : currentAmount - amount;
                });
        
        // PointUpdateService의 updateUserPoint 메서드에 대한 목 행위를 정의한다.
        // 메서드가 호출되었을 때 인자로 전달된 사용자 ID와 새 금액을 사용하여 UserPoint 객체를 반환한다.
        when(pointUpdateService.updateUserPoint(anyLong(), anyLong()))
                .thenAnswer(invocation -> UserPoint.of(invocation.getArgument(0), invocation.getArgument(1)));
        
        // PointUpdateService의 updatePointHistory 메서드에 대한 목 행위를 정의한다.
        // 메서드가 호출되었을 때 인자로 전달된 사용자 ID, 금액, 트랜잭션 타입, 현재 시간을 사용하여 PointHistory 객체를 반환한다.
        when(pointUpdateService.updatePointHistory(anyLong(), anyLong(), any(TransactionType.class)))
                .thenAnswer(invocation -> PointHistory.of(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), System.currentTimeMillis()));
    }

    @Test
    public void testChargePoint_NegativeAmount_ThrowsException() {
        // 음수 금액으로 포인트 충전 시 예외가 발생하는지 테스트
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.updatePointAndHistory(1L, -100L, TransactionType.CHARGE);
        });
    }

    @Test
    public void testUsePoint_NegativeAmount_ThrowsException() {
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
        // 포인트 충전 테스트
        long id = 1L;
        long amount = 100L;
        UserPoint userPoint = UserPoint.of(id, 0L);

        when(userPointTable.selectById(id)).thenReturn(userPoint);
        when(pointUpdateService.calculateNewAmount(eq(userPoint.point()), eq(amount), eq(TransactionType.CHARGE)))
                .thenReturn(userPoint.point() + amount);
        when(pointUpdateService.updateUserPoint(eq(id), eq(userPoint.point() + amount)))
                .thenReturn(UserPoint.of(id, userPoint.point() + amount));
        when(pointUpdateService.updatePointHistory(eq(id), eq(amount), eq(TransactionType.CHARGE)))
                .thenReturn(PointHistory.of(id, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        UserPoint result = pointService.updatePointAndHistory(id, amount, TransactionType.CHARGE);

        assertEquals(amount, result.point());
    }

    @Test
    public void testUpdatePointAndHistory_Use() {
        // 포인트 사용 테스트
        long id = 1L;
        long amount = 50L;
        UserPoint userPoint = UserPoint.of(id, 100L);

        when(userPointTable.selectById(id)).thenReturn(userPoint);
        when(pointUpdateService.calculateNewAmount(eq(userPoint.point()), eq(amount), eq(TransactionType.USE)))
                .thenReturn(userPoint.point() - amount);
        when(pointUpdateService.updateUserPoint(eq(id), eq(userPoint.point() - amount)))
                .thenReturn(UserPoint.of(id, userPoint.point() - amount));
        when(pointUpdateService.updatePointHistory(eq(id), eq(amount), eq(TransactionType.USE)))
                .thenReturn(PointHistory.of(id, amount, TransactionType.USE, System.currentTimeMillis()));

        UserPoint result = pointService.updatePointAndHistory(id, amount, TransactionType.USE);

        assertEquals(100L - amount, result.point());
    }

    @Test
    public void testUpdatePointAndHistory_InvalidId() {
        // 유효하지 않은 아이디으로 포인트 업데이트 시 예외가 발생하는지 테스트
        long id = -1L;
        long amount = 100L;

        assertThrows(IllegalArgumentException.class, () -> pointService.updatePointAndHistory(id, amount, TransactionType.CHARGE));
    }

    @Test
    public void testUpdatePointAndHistory_InvalidAmount() {
        // 유효하지 않은 금액으로 포인트 업데이트 시 예외가 발생하는지 테스트
        long id = 1L;
        long amount = -100L;

        assertThrows(IllegalArgumentException.class, () -> pointService.updatePointAndHistory(id, amount, TransactionType.CHARGE));
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
        // given: 테스트를 위한 초기 설정
        long id = 1L;
        UserPoint initialUserPoint = UserPoint.of(id, 100L); // 초기 포인트를 충분히 설정
        when(userPointTable.selectById(id)).thenReturn(initialUserPoint);
        when(userPointTable.insertOrUpdate(eq(id), anyLong())).thenAnswer(invocation -> UserPoint.of(id, (Long) invocation.getArguments()[1]));
        when(pointHistoryTable.insert(eq(id), anyLong(), eq(TransactionType.CHARGE), anyLong()))
                .thenAnswer(invocation -> PointHistory.of(id, (Long) invocation.getArguments()[1], TransactionType.CHARGE, (Long) invocation.getArguments()[3]));
        when(pointHistoryTable.insert(eq(id), anyLong(), eq(TransactionType.USE), anyLong()))
                .thenAnswer(invocation -> PointHistory.of(id, (Long) invocation.getArguments()[1], TransactionType.USE, (Long) invocation.getArguments()[3]));

        // Mock pointUpdateService.calculateNewAmount to avoid side effects
        when(pointUpdateService.calculateNewAmount(anyLong(), anyLong(), eq(TransactionType.CHARGE)))
                .thenReturn(initialUserPoint.point() + 100L);
        when(pointUpdateService.calculateNewAmount(anyLong(), anyLong(), eq(TransactionType.USE)))
                .thenReturn(initialUserPoint.point() - 10L);

        // 충전 작업과 사용 작업을 동시에 실행할 Callable 객체 생성
        Callable<UserPoint> chargeTask = () -> {
            try {
                return pointService.updatePointAndHistory(id, 100L, TransactionType.CHARGE);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };
        Callable<UserPoint> useTask = () -> {
            try {
                return pointService.updatePointAndHistory(id, 10L, TransactionType.USE);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };

        // 두 개의 스레드 풀을 사용하여 동시에 실행
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<UserPoint> chargeFuture = executorService.submit(chargeTask);
        Future<UserPoint> useFuture = executorService.submit(useTask);

        // 두 작업의 결과를 가져옴
        UserPoint chargeResult = chargeFuture.get();
        UserPoint useResult = useFuture.get();

        // 로그 출력
        System.out.println("Charge Result: " + (chargeResult != null ? chargeResult.point() : "null"));
        System.out.println("Use Result: " + (useResult != null ? useResult.point() : "null"));

        // 동시성 테스트의 결과를 검증
        assertNotNull(chargeResult, "Charge result should not be null");
        assertNotNull(useResult, "Use result should not be null");
        assertTrue(chargeResult.point() >= 0, "Charge result point should be non-negative");
        assertTrue(useResult.point() >= 0, "Use result point should be non-negative");

        // 스레드 풀 종료
        executorService.shutdown();
    }
}
