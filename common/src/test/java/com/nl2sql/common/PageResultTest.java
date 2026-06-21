package com.nl2sql.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult - 统一分页结果")
class PageResultTest {

    @Test
    @DisplayName("of 工厂应正确计算总页数")
    void shouldCalculatePages() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 10, 1, 2);
        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(10);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("empty 工厂应返回空记录")
    void shouldReturnEmptyPage() {
        PageResult<String> result = PageResult.empty(1, 10);
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("pageSize 为 0 时总页数应为 0")
    void shouldHandleZeroPageSize() {
        PageResult<String> result = PageResult.of(List.of(), 5, 1, 0);
        assertThat(result.getPages()).isEqualTo(0);
    }
}
