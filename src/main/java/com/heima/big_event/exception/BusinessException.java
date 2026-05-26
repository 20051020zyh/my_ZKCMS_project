package com.heima.big_event.exception;

import lombok.Getter;
import org.apache.ibatis.annotations.Param;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.web.bind.annotation.RequestParam;

//自定义业务异常(限流,业务逻辑错误)
    @Getter
    public class BusinessException extends RuntimeException {
        //错误码
        private final Integer code;

        /*
         构造方法：传入错误码 + 错误信息
         @param code 错误码（如429限流、400参数错误）
         @param msg 错误提示信息
         */

        public BusinessException(Integer code, String msg){
            super(msg);
            this.code = code;

    }
    // 只带消息的构造（默认错误码1）
    public BusinessException(String msg) {
        this(1, msg);
    }
}
