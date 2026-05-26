package com.heima.big_event.utils.Others;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AliOssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    /**
     * 上传文件到阿里云 OSS
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名
     * @return 文件访问 URL
     */
    public String uploadFile(InputStream inputStream, String originalFilename) {
        // 生成新文件名（避免重名）
        String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));

        // 创建 OSS 客户端实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 上传文件到 OSS
            ossClient.putObject(bucketName, fileName, inputStream);
            // 返回文件访问 URL
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } finally {
            // 关闭 OSS 客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
