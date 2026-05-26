package com.heima.big_event.controller.common;

import com.heima.big_event.pojo.Result;
import com.heima.big_event.utils.Others.AliOssUtil;
import com.heima.big_event.utils.Permission.RequirePermission;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileUploadController {
    private final AliOssUtil aliOssUtil;

    public FileUploadController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    @PostMapping("/upload")
    @RequirePermission("/upload")
    public Result<String> upload(MultipartFile file) throws IOException {
        //原文件的内容
        String originalFilename = file.getOriginalFilename();

        // 调用OSS工具类上传文件，直接拿到访问地址
        String url = aliOssUtil.uploadFile(file.getInputStream() , originalFilename);

        //返回OSS的访问地址，替换原来的假地址
        return Result.success(url);
    }
}
