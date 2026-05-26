package com.heima.big_event.anno;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StateValidator implements ConstraintValidator<State , String> {
    //ConstraintValidator<State , String>中的state意思是给哪个注释提供校验规则
    //string的意思是校验的数据类型
    @Override
    public boolean isValid(String value, ConstraintValidatorContext Context) {
        if (value == null){
            return false;
        }

        //走到这一步说明值不是空的
        if (value.equals("已发布") || value.equals("草稿")){
            return true;
        }
        return false;
    }
}
