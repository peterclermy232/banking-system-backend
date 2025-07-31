package com.sacco.banking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationCountResponse {
    private long unreadCount;
    private long totalCount;
    private long highPriorityCount;
}