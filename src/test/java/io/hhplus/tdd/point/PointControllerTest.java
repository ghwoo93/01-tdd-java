package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerTest {

    // 테스트 대상인 PointController를 직접 생성
    private PointController pointController;

    @Autowired
    private MockMvc mockMvc; // MockMvc를 사용하여 컨트롤러를 테스트

    @MockBean
    private PointService pointService; // PointService를 Mock으로 생성

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        pointController = new PointController(pointService); // PointController 초기화

        // Mock 설정: 특정 userId에 대해 getUserPoint 호출 시 UserPoint 반환
        UserPoint userPoint = new UserPoint(1L, 100L, 0L);
        when(pointService.getUserPoint(1L)).thenReturn(userPoint);

        // Mock 설정: 특정 userId에 대해 findPointHistoriesByUserId 호출 시 PointHistory 리스트 반환
        PointHistory pointHistory = PointHistory.of(1L, 1L, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointService.findPointHistoriesByUserId(1L)).thenReturn(Collections.singletonList(pointHistory));

        // Mock 설정: 특정 userId에 대해 updatePointAndHistory 호출 시 UserPoint 반환
        when(pointService.updatePointAndHistory(1L, 100L, TransactionType.CHARGE)).thenReturn(userPoint);
        when(pointService.updatePointAndHistory(1L, 50L, TransactionType.USE)).thenReturn(new UserPoint(1L, 50L, 0L));
    }

    @Test
    public void testGetUserPoint() throws Exception {
        // 특정 userId에 대해 getUserPoint 엔드포인트 호출 및 검증
        mockMvc.perform(get("/point/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.point").value(100L));
    }

    @Test
    public void testGetPointHistories() throws Exception {
        // 특정 userId에 대해 getPointHistories 엔드포인트 호출 및 검증
        mockMvc.perform(get("/point/1/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].point").value(100L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));
    }

    @Test
    public void testChargePoint() throws Exception {
        // 특정 userId에 대해 chargePoint 엔드포인트 호출 및 검증
        mockMvc.perform(patch("/point/1/charge").content("100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.point").value(100L));
    }

    @Test
    public void testUsePoint() throws Exception {
        // 특정 userId에 대해 usePoint 엔드포인트 호출 및 검증
        mockMvc.perform(patch("/point/1/use").content("50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.point").value(50L));
    }

    @Test
    public void testPoint_NonExistingId() {
        // 존재하지 않는 userId에 대해 getUserPoint 호출 시 기본값 반환 검증
        long id = 1L;
        when(pointService.getUserPoint(id)).thenReturn(UserPoint.of(id, 0L));

        UserPoint result = pointController.point(id);

        assertEquals(0, result.point());
    }

    @Test
    public void testHistory_NonExistingId() {
        // 존재하지 않는 userId에 대해 findPointHistoriesByUserId 호출 시 빈 리스트 반환 검증
        long id = 1L;
        when(pointService.findPointHistoriesByUserId(id)).thenReturn(Collections.emptyList());

        var result = pointController.history(id);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCharge_NegativeAmount() {
        // 음수 금액으로 chargePoint 호출 시 예외 발생 검증
        long id = 1L;
        long amount = -100L;

        assertThrows(ResponseStatusException.class, () -> pointController.charge(id, amount));
    }

    @Test
    public void testUse_NegativeAmount() {
        // 음수 금액으로 usePoint 호출 시 예외 발생 검증
        long id = 1L;
        long amount = -100L;

        assertThrows(ResponseStatusException.class, () -> pointController.use(id, amount));
    }

    @Test
    public void testUse_AmountGreaterThanCurrentPoints() {
        // 사용 금액이 현재 포인트보다 클 경우 예외 발생 검증
        long id = 1L;
        long amount = 100L;
        when(pointService.getUserPoint(id)).thenReturn(UserPoint.of(id, 50L));

        assertThrows(ResponseStatusException.class, () -> pointController.use(id, amount));
    }
}
