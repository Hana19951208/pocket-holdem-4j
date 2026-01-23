package com.pocketholdem.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("筹码计算器测试")
class ChipCalculatorTest {

    @Test
    @DisplayName("正常加法应该成功")
    void normalAdditionShouldSucceed() {
        assertEquals(300, ChipCalculator.safeAdd(100, 200));
    }

    @Test
    @DisplayName("溢出加法应该抛出异常")
    void overflowAdditionShouldThrowException() {
        int max = Integer.MAX_VALUE;
        // MAX_VALUE + 1 应该抛出异常
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeAdd(max, 1));
    }

    @Test
    @DisplayName("正常减法应该成功")
    void normalSubtractionShouldSucceed() {
        assertEquals(100, ChipCalculator.safeSubtract(300, 200));
    }

    @Test
    @DisplayName("下溢减法应该抛出异常")
    void underflowSubtractionShouldThrowException() {
        int min = Integer.MIN_VALUE;
        // MIN_VALUE - 1 应该抛出异常
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeSubtract(min, 1));
    }

    @Test
    @DisplayName("正常乘法应该成功")
    void normalMultiplicationShouldSucceed() {
        assertEquals(600, ChipCalculator.safeMultiply(100, 6));
    }

    @Test
    @DisplayName("溢出乘法应该抛出异常")
    void overflowMultiplicationShouldThrowException() {
        int max = Integer.MAX_VALUE / 2;
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeMultiply(max, 3));
    }

    @Test
    @DisplayName("正常除法应该成功")
    void normalDivisionShouldSucceed() {
        assertEquals(50, ChipCalculator.safeDivide(100, 2));
    }

    @Test
    @DisplayName("除零应该抛出异常")
    void divisionByZeroShouldThrowException() {
        assertThrows(ArithmeticException.class,
            () -> ChipCalculator.safeDivide(100, 0));
    }
}
