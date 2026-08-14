package com.campus.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;

/**
 * 邮箱验证码工具
 * 基于 Redis 存储，替代原 HttpSession 方案，支持无状态部署
 */
@Component
public class EmailCodeUtil {

    private static final String CODE_PREFIX = "email_code:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailCodeUtil(StringRedisTemplate redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    public String generateCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    public boolean sendCodeEmail(String toEmail, String code, String subject) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText("您的验证码：" + code + "，有效期5分钟，请尽快使用。");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 存储验证码到 Redis，5 分钟自动过期 */
    public void storeCode(String businessType, String key, String code) {
        redisTemplate.opsForValue().set(CODE_PREFIX + businessType + ":" + key, code, TTL);
    }

    /** 获取存储的验证码 */
    public String getCode(String businessType, String key) {
        return redisTemplate.opsForValue().get(CODE_PREFIX + businessType + ":" + key);
    }

    /** 删除验证码 */
    public void removeCode(String businessType, String key) {
        redisTemplate.delete(CODE_PREFIX + businessType + ":" + key);
    }

    /**
     * 校验验证码，通过后自动删除
     * @return null 表示通过，否则返回错误信息
     */
    public String verifyCode(String businessType, String key, String inputCode) {
        String storedCode = getCode(businessType, key);
        if (storedCode == null) {
            return "验证码已过期，请重新获取";
        }
        if (!inputCode.equals(storedCode)) {
            return "验证码不正确";
        }
        removeCode(businessType, key);
        return null;
    }
}
