package com.heima.big_event.controller.common;


import com.heima.big_event.exception.BusinessException;
import com.heima.big_event.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

//@RestControllerAdvice:这是springMVC提供的注解,结合了@ControllerAdvice和@ResponseBody的功能
//能捕获所有@RestController或者@Controller中抛出的异常
//并直接将返回值序列化为JSON响应
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //限流自定义异常处理
    // 新增：限流自定义异常处理
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }


    //指定这个方法专门处理MethodArgumentNotValidException类型的异常
    //这种异常通常在控制器方法参数使用@Vaild或@Validated注解进行校验且校验失败时抛出
    @ExceptionHandler(MethodArgumentNotValidException.class)
    //设置响应状态码为400(客户端错误),表示请求参数有问题
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    //1.
    //handleValidationExceptions方法专门处理MethodArgumentNotValidException类型的异常
    //接收这个异常对象作为参数,以便从中提取错误信息
    public Result handleValidationExceptions(MethodArgumentNotValidException ex) {
        //2.
        //创建一个HashMap对象,键(key)和值(value)都是string类型
        //这个Map用于临时存储从异常中解析出的所有字段错误
        //键(fieldName) : 发生校验失败的字段名 , 例如"username"
        //值(errorMessage) : 该字段对应的校验失败提示信息, 例如"用户名不能为空"
        Map<String , String> errors = new HashMap<>();
        //3.
        //从异常对象ex中获取绑定结果BindingResult,它封装了数据绑定和校验的所有信息
        //getAllErrors() : 获取绑定过程中产生的所有错误对象(因为是所有,又是HashMap对象,所以用list接收),
        //forEach() : 这是Java8的流式api方法,用于遍历 getAllErrors()返回的这个错误列表
        //error -> 是一个Lambda表达式,'error' 是遍历列表时每个错误元素的临时遍历名
        ex.getBindingResult().getAllErrors().forEach(error -> {
            //((FieldError) error),将ObjectError对象向下转型为跟具体的FieldErrord对象
            //getField() : 获取校验失败的字段名
            String fieldName = ((FieldError) error).getField();
            //getDefaultMessage() : 获取字段上的错误提示
            String errorMessage = error.getDefaultMessage();
            //将字段名和错误信息作为键值对，存入之前创建的 `errors` Map 中。
            //如果 同一个字段有多个校验规则失败, 这里会以最后一次遍历到的错误信息为准(因为Map键是唯一的)
            errors.put(fieldName, errorMessage);
        });//至此,Lambda表示结束,所以字段错误信息都以提取并存入'errors'Map


        //假设你的Result类有静态方法error (String message , Object data)
        //如果只有error(String message) ,可以只返回message,或自定义格式
        return Result.error("参数校验失败" + errors);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        Object value = e.getValue();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知";
        return Result.error("参数 '" + name + "' 的值 '" + value + "' 无法转换为 " + requiredType);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数: " + e.getParameterName());
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleAllUncaughtException(Exception e){
        //打印完整的异常日志
        log.error("系统异常" , e);
        //返回友好提示
        return Result.error("系统繁忙,请稍后再试");
    }
}
