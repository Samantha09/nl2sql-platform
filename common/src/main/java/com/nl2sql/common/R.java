package com.nl2sql.common;

import com.nl2sql.common.enums.IResultCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<T>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> error(String message) {
        return error(500, message);
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<T>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /** 基于结果码构造错误响应，message 取国际化文案（按当前 locale） */
    public static <T> R<T> error(IResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }
}

