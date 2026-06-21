package com.nl2sql.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultCode - 通用错误码")
class ResultCodeTest {

    @Test
    @DisplayName("所有枚举应携带正确的 code、i18nKey、desc")
    void shouldHaveCorrectMetadata() {
        assertThat(ResultCode.SUCCESS.getCode()).isEqualTo(200);
        assertThat(ResultCode.SUCCESS.getI18nKey()).isEqualTo("result.success");
        assertThat(ResultCode.SUCCESS.getDesc()).isEqualTo("操作成功");

        assertThat(ResultCode.BAD_REQUEST.getCode()).isEqualTo(400);
        assertThat(ResultCode.BAD_REQUEST.getI18nKey()).isEqualTo("result.bad_request");
        assertThat(ResultCode.BAD_REQUEST.getDesc()).isEqualTo("请求参数错误");

        assertThat(ResultCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(ResultCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(ResultCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(ResultCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
        assertThat(ResultCode.SERVICE_UNAVAILABLE.getCode()).isEqualTo(503);
    }
}
