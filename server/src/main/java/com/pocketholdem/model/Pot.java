package com.pocketholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 底池信息（不可变Record）
 * 代表一个主池或边池。
 * 参考：numeric-spec.md 第6章
 * 
 * @param amount            底池金额
 * @param eligiblePlayerIds 有资格分配该底池的玩家ID列表
 */
public record Pot(
        @JsonProperty("amount") int amount,
        @JsonProperty("eligible_player_ids") String[] eligiblePlayerIds
) {}
