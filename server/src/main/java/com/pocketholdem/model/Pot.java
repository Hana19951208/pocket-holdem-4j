package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 底池信息（不可变Record）
 * 参考：numeric-spec.md 第6章
 */
public record Pot(
        @JsonProperty("amount") int amount,
        @JsonProperty("eligible_player_ids") String[] eligiblePlayerIds
) {}
