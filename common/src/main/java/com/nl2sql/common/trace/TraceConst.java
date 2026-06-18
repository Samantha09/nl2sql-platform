package com.nl2sql.common.trace;

/**
 * 链路追踪常量。
 */
public final class TraceConst {

    private TraceConst() {}

    /** HTTP 头与 MDC 中的 traceId 键名 */
    public static final String TRACE_ID = "traceId";

    /** HTTP 请求头名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}
