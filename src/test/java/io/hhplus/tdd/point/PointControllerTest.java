package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.hhplus.tdd.database.UserPointTable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerTest {

    // 테스트 대상인 PointController를 직접 생성
    private PointController pointController;

    @Autowired
    private MockMvc mockMvc; // MockMvc를 사용하여 컨트롤러를 테스트

    @MockBean
    private PointService pointService; // PointService를 Mock으로 생성

    @Test
    public void testPoint_NonExistingId() {
        // given
        MockitoAnnotations.openMocks(this);
        pointController = new PointController(pointService);
        long id = 1L;
        when(pointService.getUserPoint(id)).thenReturn(UserPoint.of(id, 0L));

        // when
        UserPoint result = pointController.point(id);

        // then
        assertEquals(0, result.point());
    }

    @Test
    public void testHistory_NonExistingId() {
        // given
        MockitoAnnotations.openMocks(this);
        pointController = new PointController(pointService);
        long id = 1L;
        when(pointService.findPointHistoriesByUserId(id)).thenReturn(Collections.emptyList());

        // when
        var result = pointController.history(id);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUse_InvalidAmountUse() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        when(pointService.updatePointAndHistory(userId, invalidAmount, TransactionType.USE))
                .thenThrow(new IllegalArgumentException("Invalid amount"));

        
        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(invalidAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"400\",\"message\":\"Invalid amount\"}"));
    }

    @Test
    public void testUse_InvalidAmountCharge() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        when(pointService.updatePointAndHistory(userId, invalidAmount, TransactionType.CHARGE))
                .thenThrow(new IllegalArgumentException("Invalid amount"));

        
        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(invalidAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"400\",\"message\":\"Invalid amount\"}"));
    }

    @Test
    public void testUse_AmountGreaterThanCurrentPoints() throws Exception {
        // Mock UserPointTable
        UserPointTable userPointTable = mock(UserPointTable.class);

        // given
        long userId = 1L;
        long invalidAmount = 100L;
        long currentPoints = 50L;
        UserPoint userPoint = UserPoint.of(userId, currentPoints);

        // 목킹된 사용자 포인트 테이블이 주어진 사용자 아이디에 대한 사용자 포인트를 반환하도록 설정
        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        // 목킹된 서비스가 유효하지 않은 금액에 대한 예의를 던지도록 설정
        when(pointService.updatePointAndHistory(userId, invalidAmount, TransactionType.USE))
                .thenThrow(new IllegalArgumentException("Insufficient balance"));

        
        // 
        mockMvc.perform(patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(invalidAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"400\",\"message\":\"Insufficient balance\"}"));
    }
}
