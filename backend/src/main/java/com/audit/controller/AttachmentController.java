package com.audit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.audit.common.Result;
import com.audit.mapper.AttachmentMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/audit/v1")
public class AttachmentController {

    private final AttachmentMapper mapper;
    private final String uploadDir = new java.io.File("").getAbsolutePath() + java.io.File.separator + "uploads";

    public AttachmentController(AttachmentMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/plan/batches/{batchId}/attachments")
    public Result<Map<String, Object>> list(@PathVariable String batchId) {
        List<Map<String, Object>> list = mapper.findByBatchId(batchId);
        // 格式化 file_size 为 KB
        for (Map<String, Object> m : list) {
            Object size = m.get("file_size");
            if (size instanceof Number) {
                long bytes = ((Number) size).longValue();
                m.put("fileSize", bytes);
                // 保留原始字段兼容下划线
            }
            // 驼峰映射
            if (m.containsKey("attach_id")) m.put("attachId", m.get("attach_id"));
            if (m.containsKey("file_name")) m.put("fileName", m.get("file_name"));
            if (m.containsKey("file_type")) m.put("fileType", m.get("file_type"));
            if (m.containsKey("attach_type")) m.put("attachType", m.get("attach_type"));
            if (m.containsKey("attach_type_name")) m.put("attachTypeName", m.get("attach_type_name"));
            if (m.containsKey("upload_time")) m.put("uploadTime", m.get("upload_time"));
            if (m.containsKey("upload_by")) m.put("uploadBy", m.get("upload_by"));
        }
        return Result.ok(Map.of("list", list));
    }

    @PostMapping("/plan/batches/{batchId}/attachments")
    public Result<Map<String, Object>> upload(@PathVariable String batchId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "attachType", defaultValue = "OTHER") String attachType) {
        try {
            String attachId = "ATT" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".") ?
                    originalName.substring(originalName.lastIndexOf(".")) : "";
            String savedName = attachId + ext;

            // 保存文件
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new File(dir, savedName));

            // 附件类型映射
            Map<String, String> typeNames = Map.of(
                    "DELEGATION", "审计委托书", "RESOLUTION", "审批决议",
                    "TASK", "上级任务文件", "RESEARCH", "调研材料", "OTHER", "其他"
            );

            mapper.insert(attachId, batchId, originalName, file.getSize(),
                    file.getContentType(), attachType,
                    typeNames.getOrDefault(attachType, "其他"), "当前用户",
                    savedName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("attachId", attachId);
            result.put("fileName", originalName);
            result.put("fileSize", file.getSize());
            result.put("attachType", attachType);
            result.put("attachTypeName", typeNames.getOrDefault(attachType, "其他"));
            result.put("previewUrl", "/api/audit/v1/files/" + attachId + "/preview");
            result.put("downloadUrl", "/api/audit/v1/files/" + attachId + "/download");
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(500, "上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/plan/batches/{batchId}/attachments/{attachId}")
    public Result<Void> delete(@PathVariable String batchId, @PathVariable String attachId) {
        Map<String, Object> att = mapper.findByAttachId(attachId);
        if (att != null && att.get("file_path") != null) {
            FileUtil.del(new File(uploadDir, att.get("file_path").toString()));
        }
        mapper.delete(attachId, batchId);
        return Result.ok();
    }

    @GetMapping("/files/{attachId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable String attachId) {
        return serveFile(attachId, true);
    }

    @GetMapping("/files/{attachId}/download")
    public ResponseEntity<Resource> download(@PathVariable String attachId) {
        return serveFile(attachId, false);
    }

    private ResponseEntity<Resource> serveFile(String attachId, boolean inline) {
        Map<String, Object> att = mapper.findByAttachId(attachId);
        if (att == null) return ResponseEntity.notFound().build();

        String filePath = (String) att.get("file_path");
        String fileName = (String) att.get("file_name");
        File file = new File(uploadDir, filePath);
        if (!file.exists()) return ResponseEntity.notFound().build();

        // 无法内联预览的类型：强制下载
        String ext = fileName != null && fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
        Set<String> nonPreviewable = Set.of(".xlsx", ".xls", ".docx", ".doc", ".zip", ".rar");
        if (inline && nonPreviewable.contains(ext)) {
            inline = false; // Office 文件无法在浏览器中内联预览，转为下载
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(inline ?
                org.springframework.http.ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build() :
                org.springframework.http.ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
