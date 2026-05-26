package com.heima.big_event.pojo.dto;
import org.springframework.util.StringUtils;

/**
 * 密码相关参数校验工具类
 */
public class PasswordDTO {

    // 密码长度规则（和注册保持一致：5~16位）
    private static final int PWD_MIN_LENGTH = 5;
    private static final int PWD_MAX_LENGTH = 16;

    /**
     * 校验修改密码的参数
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @param rePwd 确认新密码
     * @param userName 用户名
     * @return 校验通过返回null，失败返回错误提示
     */
    public static String validateUpdatePwdParams(String oldPwd, String newPwd, String rePwd, String userName) {
        // 1. 非空校验
        if (!StringUtils.hasText(oldPwd)) {
            return "旧密码不能为空";
        }
        if (!StringUtils.hasText(newPwd)) {
            return "新密码不能为空";
        }
        if (!StringUtils.hasText(rePwd)) {
            return "确认密码不能为空";
        }
        if (!StringUtils.hasText(userName)) {
            return "用户信息不存在，请重新登录";
        }

        // 2. 密码长度校验
        if (newPwd.length() < PWD_MIN_LENGTH || newPwd.length() > PWD_MAX_LENGTH) {
            return String.format("新密码长度必须为%d~%d位字符", PWD_MIN_LENGTH, PWD_MAX_LENGTH);
        }

        // 3. 两次密码一致性校验
        if (!newPwd.equals(rePwd)) {
            return "两次输入的新密码不一致";
        }

        // 4. 新密码不能和旧密码相同（可选）
        if (newPwd.equals(oldPwd)) {
            return "新密码不能与旧密码相同";
        }

        // 校验通过
        return null;
    }
}
