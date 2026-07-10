package com.audit.common;

/**
 * 统一业务状态码常量
 * <p>200=成功, 4xx=业务错误, 1xxx=鉴权权限错误</p>
 */
public final class ResultCode {

    private ResultCode() {}

    /** 操作成功 */
    public static final int SUCCESS = 200;

    /** 参数校验失败 */
    public static final int BAD_REQUEST = 400;

    /** 用户名或密码错误 */
    public static final int UNAUTHORIZED = 401;

    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 500;

    // ==================== 鉴权专用错误码 ====================

    /** 未登录（Token缺失） */
    public static final int NOT_LOGIN = 1001;

    /** Token已过期 */
    public static final int TOKEN_EXPIRED = 1002;

    /** Token非法（签名/格式错误） */
    public static final int TOKEN_INVALID = 1003;

    /** 角色不足（缺少所需角色） */
    public static final int ROLE_DENIED = 1004;

    /** 无接口访问权限（缺少所需权限标识） */
    public static final int PERM_DENIED = 1005;

    // ==================== 验证码专用错误码 ====================

    /** 验证码不存在或已过期 */
    public static final int CAPTCHA_EXPIRED = 1006;

    /** 验证码输入错误 */
    public static final int CAPTCHA_ERROR = 1007;
}
