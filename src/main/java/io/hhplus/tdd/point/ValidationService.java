package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid ID");
        }
    }

    public void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    public void validateTransactionType(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Invalid transaction type");
        }
    }
}
