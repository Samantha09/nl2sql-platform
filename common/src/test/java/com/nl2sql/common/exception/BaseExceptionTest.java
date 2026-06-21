package com.nl2sql.common.exception;

import com.nl2sql.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseException - 业务异常父类")
class BaseExceptionTest {

    @Test
    @DisplayName("以结果码构造时应取结果码描述与 code")
    void shouldBuildFromResultCode() {
        BaseException ex = new BaseException(ResultCode.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("请求参数错误");
    }

    @Test
    @DisplayName("以结果码+自定义消息构造时应覆盖默认描述")
    void shouldBuildFromResultCodeWithCustomMessage() {
        BaseException ex = new BaseException(ResultCode.BAD_REQUEST, "参数不合法");
        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("参数不合法");
    }

    @Test
    @DisplayName("以自定义 code+消息构造时应保留传入值")
    void shouldBuildFromCustomCode() {
        BaseException ex = new BaseException(999, "custom");
        assertThat(ex.getCode()).isEqualTo(999);
        assertThat(ex.getMessage()).isEqualTo("custom");
    }

    @Test
    @DisplayName("仅以消息构造时应使用 500 默认 code")
    void shouldUseDefault500Code() {
        BaseException ex = new BaseException("server error");
        assertThat(ex.getCode()).isEqualTo(500);
        assertThat(ex.getMessage()).isEqualTo("server error");
    }
}
