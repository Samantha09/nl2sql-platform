package com.nl2sql.common.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 业务枚举统一父接口。所有需要「编码 + 描述」语义的枚举都实现它，
 * 以获得一致的取值方式与按编码反查能力。
 * <p>Java 枚举无法继承类，故以接口作为「枚举父类」。
 *
 * @param <C> 编码类型（如 Integer、String）
 */
public interface IEnum<C> {

    /** 枚举编码（持久化 / 传输用的稳定值） */
    C getCode();

    /** 枚举描述（展示用的可读文本） */
    String getDesc();

    /**
     * 按编码反查枚举值。
     *
     * @param type 枚举类型
     * @param code 目标编码
     * @return 匹配的枚举，找不到返回空
     */
    static <C, E extends Enum<E> & IEnum<C>> Optional<E> of(Class<E> type, C code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> code.equals(e.getCode()))
                .findFirst();
    }
}
