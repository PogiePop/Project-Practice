package com.audit.common;

public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "操作成功";
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> ok() { return ok(null); }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    // ==================== 鉴权专用快捷方法 ====================

    /** 未登录 */
    public static <T> Result<T> notLogin() {
        return fail(ResultCode.NOT_LOGIN, "未登录，请先登录");
    }

    /** Token已过期 */
    public static <T> Result<T> tokenExpired() {
        return fail(ResultCode.TOKEN_EXPIRED, "Token已过期，请重新登录");
    }

    /** Token非法 */
    public static <T> Result<T> tokenInvalid() {
        return fail(ResultCode.TOKEN_INVALID, "Token非法");
    }

    /** 角色不足 */
    public static <T> Result<T> roleDenied() {
        return fail(ResultCode.ROLE_DENIED, "角色权限不足");
    }

    /** 无接口操作权限 */
    public static <T> Result<T> permDenied() {
        return fail(ResultCode.PERM_DENIED, "无此操作权限");
    }

    /** 登录失败（用户名或密码错误） */
    public static <T> Result<T> loginFailed() {
        return fail(ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
