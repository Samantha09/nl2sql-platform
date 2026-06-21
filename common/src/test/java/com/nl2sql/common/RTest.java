package com.nl2sql.common;

import com.nl2sql.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("R - 统一响应封装")
class RTest {

    @Test
    @DisplayName("ok() 应返回 code=200, message=success, data=null")
    void shouldReturnSuccessWithoutData() {
        R<Void> r = R.ok();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMessage()).isEqualTo("success");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("ok(data) 应携带数据")
    void shouldReturnSuccessWithData() {
        R<String> r = R.ok("hello");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMessage()).isEqualTo("success");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("error(message) 应返回 code=500")
    void shouldReturnDefaultError() {
        R<Void> r = R.error("fail");
        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMessage()).isEqualTo("fail");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("error(code, message) 应返回自定义错误码")
    void shouldReturnErrorWithCustomCode() {
        R<Void> r = R.error(404, "not found");
        assertThat(r.getCode()).isEqualTo(404);
        assertThat(r.getMessage()).isEqualTo("not found");
    }

    @Test
    @DisplayName("error(IResultCode) 应从结果码构造错误响应")
    void shouldReturnErrorFromResultCode() {
        R<Void> r = R.error(ResultCode.BAD_REQUEST);
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMessage()).isEqualTo("请求参数错误");
    }
}
