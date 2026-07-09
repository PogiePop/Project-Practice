package com.audit.controller;

import com.audit.common.Result;
import com.audit.mapper.MessageMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/audit/v1")
public class MessageController {

    private final MessageMapper mapper;

    public MessageController(MessageMapper mapper) { this.mapper = mapper; }

    @GetMapping("/messages")
    public Result<Map<String, Object>> getMessages(@RequestParam(required = false) Integer isRead,
                                                    @RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "50") int pageSize) {
        return Result.ok(Map.of("list", mapper.findList(isRead), "total", mapper.findList(isRead).size()));
    }

    @PutMapping("/messages/{messageId}/read")
    public Result<Void> markRead(@PathVariable String messageId) {
        mapper.markRead(messageId);
        return Result.ok();
    }

    @PutMapping("/messages/read-all")
    public Result<Void> markAllRead() {
        mapper.markAllRead();
        return Result.ok();
    }

    @GetMapping("/messages/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        return Result.ok(Map.of("count", mapper.countUnread()));
    }

    @PostMapping("/messages/import-notify")
    public Result<Void> importNotify(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "导入完成");
        String content = body.getOrDefault("content", "");
        String msgId = "MSG" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        mapper.insertAlert(msgId, title, content, "SYSTEM", null);
        return Result.ok();
    }
}
