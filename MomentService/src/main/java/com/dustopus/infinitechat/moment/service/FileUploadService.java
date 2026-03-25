package com.dustopus.infinitechat.moment.service;

import java.util.Map;

public interface FileUploadService {

    /** 获取预签名上传URL */
    Map<String, String> getUploadUrl(String fileName, String contentType);

    /** 获取预签名下载URL */
    String getDownloadUrl(String objectName);
}
