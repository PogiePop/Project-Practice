package com.audit.controller;

import com.audit.common.Result;
import com.audit.common.util.CaptchaUtil;
import com.audit.common.util.RedisUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图形验证码控制器
 * <p>GET /audit/v1/captcha/generate?uuid=xxx → Base64图片</p>
 */
@RestController
@RequestMapping("/audit/v1/captcha")
public class CaptchaController {

    private static final String KEY_PREFIX = "captcha:";
    private static final long TTL_SECONDS = 120; // 2分钟过期

    private final RedisUtil redisUtil;

    public CaptchaController(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 生成图形验证码
     * @param uuid 客户端随机生成的唯一标识
     * @return { captchaImage: "data:image/png;base64,..." }
     */
    @GetMapping("/generate")
    public Result<Map<String, String>> generate(@RequestParam String uuid) {
        // 1. 生成4位验证码文本
        String code = CaptchaUtil.generateCode();
        // 2. 存入Redis，大写存储（比对时不区分大小写）
        redisUtil.setEx(KEY_PREFIX + uuid, code.toUpperCase(), TTL_SECONDS);
        // 3. 生成干扰图形Base64
        String image = CaptchaUtil.generateImage(code);
        return Result.ok(Map.of("captchaImage", image));
    }
}
