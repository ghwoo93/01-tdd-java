package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
@AutoConfigureMockMvc
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    public void testPoint_NonExistingId() throws Exception {
        // given
        long id = 1L;
        when(pointService.getUserPoint(id)).thenReturn(UserPoint.of(id, 0L));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/point/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(0));
    }

    @Test
    public void testHistory_NonExistingId() throws Exception {
        // given
        long id = 1L;
        when(pointService.findPointHistoriesByUserId(id)).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/point/{id}/histories", id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
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
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"500\",\"message\":\"Invalid amount\"}"));
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
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"500\",\"message\":\"Invalid amount\"}"));
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

        
        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(invalidAmount)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":\"500\",\"message\":\"Insufficient balance\"}"));
    }
}
