package com.pocketholdem.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 筹码计算工具类
 * 所有运算都进行强制溢出检查，防止整数溢出导致的漏洞。
 * 参考：docs/numeric-spec.md
 */
@Slf4j
public final class ChipCalculator {

    private ChipCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 安全加法，检查溢出
     */
    public static int safeAdd(int a, int b) {
        if (b > 0 && a > Integer.MAX_VALUE - b) {
            log.error("整数加法溢出: {} + {}", a, b);
            throw new ArithmeticException("整数加法溢出: " + a + " + " + b);
        }
        if (b < 0 && a < Integer.MIN_VALUE - b) {
            log.error("整数加法下溢: {} + {}", a, b);
            throw new ArithmeticException("整数加法下溢: " + a + " + " + b);
        }
        return a + b;
    }

    /**
     * 安全减法，检查溢出
     */
    public static int safeSubtract(int a, int b) {
        if (b < 0 && a > Integer.MAX_VALUE + b) {
            log.error("整数减法溢出: {} - {}", a, b);
            throw new ArithmeticException("整数减法溢出: " + a + " - " + b);
        }
        if (b > 0 && a < Integer.MIN_VALUE + b) {
            log.error("整数减法下溢: {} - {}", a, b);
            throw new ArithmeticException("整数减法下溢: " + a + " - " + b);
        }
        return a - b;
    }

    /**
     * 安全乘法，检查溢出
     */
    public static int safeMultiply(int a, int b) {
        if (a > b) {
            // 交换，减少检查次数
            return safeMultiply(b, a);
        }

        if (a < 0) {
            // 处理负数
            if (b < 0) {
                if (a < Integer.MAX_VALUE / b) {
                    log.error("整数乘法溢出: {} * {}", a, b);
                    throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
                }
            } else if (b > 0) {
                if (a < Integer.MIN_VALUE / b) {
                    log.error("整数乘法溢出: {} * {}", a, b);
                    throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
                }
            }
            // b == 0 无需检查
        } else if (a > 0) {
            if (a > Integer.MAX_VALUE / b) {
                log.error("整数乘法溢出: {} * {}", a, b);
                throw new ArithmeticException("整数乘法溢出: " + a + " * " + b);
            }
        }

        return a * b;
    }

    /**
     * 安全除法，检查除零
     */
    public static int safeDivide(int a, int b) {
        if (b == 0) {
            log.error("尝试除以零: {} / {}", a, b);
            throw new ArithmeticException("整数除以零: " + a + " / " + b);
        }
        return a / b;
    }

    /**
     * 检查是否合法增加筹码（增加不能为负）
     */
    public static void validateIncrement(int amount) {
        if (amount < 0) {
            log.error("非法筹码增加: {}", amount);
            throw new IllegalArgumentException("增加筹码不能为负数: " + amount);
        }
    }

    /**
     * 检查是否合法扣除筹码（扣除不能超过当前筹码）
     */
    public static void validateDecrement(int currentChips, int amount) {
        if (amount < 0) {
            log.error("非法筹码扣除: {}", amount);
            throw new IllegalArgumentException("扣除筹码不能为负数: " + amount);
        }
        if (amount > currentChips) {
            log.error("筹码扣除超限: 当前={}, 扣除={}", currentChips, amount);
            throw new IllegalArgumentException(
                "扣除筹码不能超过当前筹码: " + amount + " > " + currentChips
            );
        }
    }
}