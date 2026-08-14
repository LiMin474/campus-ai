package com.campus.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.util.EmailCodeUtil;
import com.campus.user.dto.EmailVerifyCodeRequest;
import com.campus.user.dto.ResetPasswordRequest;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邮箱验证码服务
 * 验证码存储于 Redis（通过 EmailCodeUtil），不再依赖 HttpSession
 */
@Service
public class EmailCodeService {

    private static final String BUSINESS_TYPE_REGISTER = "register";
    private static final String BUSINESS_TYPE_RESET_PWD = "reset_pwd";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeUtil emailCodeUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String sendRegisterCode(EmailVerifyCodeRequest request) {
        String studentNo = request.getStudentNo();
        String email = request.getEmail();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getStudentNo, studentNo));
        if (user != null) {
            return "该学号已注册，请直接登录或使用忘记密码";
        }

        String code = emailCodeUtil.generateCode(5);
        boolean success = emailCodeUtil.sendCodeEmail(email, code, "校园交易平台-注册验证码");
        if (!success) {
            return "验证码发送失败，请检查邮箱地址";
        }

        emailCodeUtil.storeCode(BUSINESS_TYPE_REGISTER, codeKey(studentNo, email), code);
        return null;
    }

    @Transactional
    public String completeRegister(String studentNo, String email, String verifyCode, String nickname, String password) {
        String key = codeKey(studentNo, email);
        String error = emailCodeUtil.verifyCode(BUSINESS_TYPE_REGISTER, key, verifyCode);
        if (error != null) {
            return error;
        }

        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getStudentNo, studentNo));
        if (existingUser != null) {
            return "该学号已注册";
        }

        User user = new User();
        user.setStudentNo(studentNo);
        user.setPhone("");
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setCreditScore(100);
        user.setCarbonPoints(0);
        user.setAppealFailCount(0);
        user.setBanned(false);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.insert(user);

        return null;
    }

    public String sendResetPwdCode(EmailVerifyCodeRequest request) {
        String studentNo = request.getStudentNo();
        String email = request.getEmail();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getStudentNo, studentNo));
        if (user == null) {
            return "学号不存在";
        }

        String code = emailCodeUtil.generateCode(5);
        boolean success = emailCodeUtil.sendCodeEmail(email, code, "校园交易平台-重置密码验证码");
        if (!success) {
            return "验证码发送失败，请检查邮箱地址";
        }

        emailCodeUtil.storeCode(BUSINESS_TYPE_RESET_PWD, codeKey(studentNo, email), code);
        return null;
    }

    public String verifyResetPwdCode(String studentNo, String email, String verifyCode) {
        return emailCodeUtil.verifyCode(BUSINESS_TYPE_RESET_PWD, codeKey(studentNo, email), verifyCode);
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        String studentNo = request.getStudentNo();
        String email = request.getEmail();
        String verifyCode = request.getVerifyCode();

        String key = codeKey(studentNo, email);
        String error = emailCodeUtil.verifyCode(BUSINESS_TYPE_RESET_PWD, key, verifyCode);
        if (error != null) {
            return error;
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getStudentNo, studentNo));
        if (user == null) {
            return "学号不存在";
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        return null;
    }

    private String codeKey(String studentNo, String email) {
        return studentNo + "|" + email;
    }
}
