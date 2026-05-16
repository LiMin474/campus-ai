package com.campus.file;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.storage-type:local}")
    private String storageType;

    @Value("${app.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${app.oss.access-key-id:}")
    private String ossAccessKeyId;

    @Value("${app.oss.access-key-secret:}")
    private String ossAccessKeySecret;

    @Value("${app.oss.bucket-name:}")
    private String ossBucketName;

    @Value("${app.oss.domain:}")
    private String ossDomain;

    private Path root;
    private OSS ossClient;

    @PostConstruct
    public void init() throws IOException {
        if ("local".equals(storageType)) {
            root = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } else if ("oss".equals(storageType)) {
            ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String original = file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        if ("local".equals(storageType)) {
            return storeLocal(file, name);
        } else if ("oss".equals(storageType)) {
            return storeOss(file, name);
        } else {
            throw new IllegalArgumentException("不支持的存储类型");
        }
    }

    private String storeLocal(MultipartFile file, String filename) throws IOException {
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法路径");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/files/" + filename;
    }

    private String storeOss(MultipartFile file, String filename) throws IOException {
        if (ossClient == null) {
            throw new IllegalStateException("OSS 客户端未初始化");
        }
        String objectName = "uploads/" + filename;
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(ossBucketName, objectName, in);
        }
        if (ossDomain != null && !ossDomain.isEmpty()) {
            return ossDomain + "/" + objectName;
        } else {
            return "https://" + ossBucketName + "." + ossEndpoint + "/" + objectName;
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
