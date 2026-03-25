package com.dustopus.infinitechat.moment.controller;

import com.dustopus.infinitechat.common.result.Result;
import com.dustopus.infinitechat.moment.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 获取预签名上传URL
     * @param fileName 文件名（含扩展名）
     * @param contentType 文件MIME类型
     */
    @GetMapping("/url")
    public Result<Map<String, String>> getUploadUrl(@RequestParam String fileName,
                                                     @RequestParam(required = false) String contentType) {
        return Result.ok(fileUploadService.getUploadUrl(fileName, contentType));
    }

    /**
     * 获取下载URL
     * @param objectName 对象名称
     */
    @GetMapping("/download")
    public Result<String> getDownloadUrl(@RequestParam String objectName) {
        return Result.ok(fileUploadService.getDownloadUrl(objectName));
    }
}
