package com.sacco.banking.dto.response;

import com.sacco.banking.entity.SavingsGoal;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
public class SavingsGoalResponse {
    private Long id;
    private String goalName;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDateTime targetDate;
    private String status;
    private BigDecimal progressPercentage;
    private LocalDateTime createdAt;

    public static SavingsGoalResponse fromEntity(SavingsGoal goal) {
        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return SavingsGoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus().name())
                .progressPercentage(progressPercentage)
                .createdAt(goal.getCreatedAt())
                .build();
    }
}