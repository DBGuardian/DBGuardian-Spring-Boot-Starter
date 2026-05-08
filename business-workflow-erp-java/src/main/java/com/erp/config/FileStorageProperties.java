package com.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

  /**
   * 存储类型：local/oss
   */
  private String type = "local";

  /**
   * 本地存储配置
   */
  private final Local local = new Local();

  /**
   * OSS 存储配置（腾讯云 COS 等对象存储服务）
   */
  private final Oss oss = new Oss();

  @Data
  public static class Local {
    /**
     * 本地存储根目录，例如：C:/erp 或 /data/erp
     */
    private String path;
  }

  @Data
  public static class Oss {
    /**
     * 对象存储服务访问域名（Endpoint）
     * 例如：cos.ap-guangzhou.myqcloud.com 或 https://cos.ap-guangzhou.myqcloud.com
     */
    private String endpoint;

    /**
     * 访问密钥 ID（SecretId）
     */
    private String accessKeyId;

    /**
     * 访问密钥 Key（SecretKey）
     */
    private String accessKeySecret;

    /**
     * 存储桶名称（Bucket Name）
     */
    private String bucketName;

    /**
     * 存储区域（Region），如：ap-guangzhou
     */
    private String region;
  }
}