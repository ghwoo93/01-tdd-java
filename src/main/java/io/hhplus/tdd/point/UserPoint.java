package io.hhplus.tdd.point;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

// DTO, DAO, ENTITY
// @Entity
public record UserPoint(
        @Id long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public static UserPoint of(long id, long amount) {
        return new UserPoint(id, amount, System.currentTimeMillis());
    }
}
