package io.hhplus.tdd.point;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// DTO, DAO, ENTITY
// @Entity
public record PointHistory(
        @Id @GeneratedValue(strategy = GenerationType.AUTO) long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {

        public static PointHistory of(long userId, long amount, TransactionType transactionType, long timestamp) {
                return new PointHistory(0L, userId, amount, transactionType, timestamp);
        }
}
