package com.sacco.banking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberStatsResponse {
    private long totalMembers;
    private long activeMembers;
    private long inactiveMembers;
    private long suspendedMembers;
}