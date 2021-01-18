package com.alibaba.nacos.common.utils;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.common.codec.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SpasAdapter {

    public static String calculateSign(String tenant, String group, String timestamp, String secretKey) {

        String resource = "";
        if (tenant != null && group != null) {
            resource += tenant + "+" +group;
        } else {
            if (!StringUtils.isBlank(group)) {
                resource = group;
            }
        }

        if (StringUtils.isBlank(resource)) {
            return signWithHmacSha1Encrypt(timestamp, secretKey);
        } else {
            return signWithHmacSha1Encrypt(resource + "+" + timestamp, secretKey);
        }

    }

    /**
     * Sign with hmac SHA1 encrtpt.
     *
     * @param encryptText encrypt text
     * @param encryptKey  encrypt key
     * @return base64 string
     */
    public static String signWithHmacSha1Encrypt(String encryptText, String encryptKey) {
        try {
            byte[] data = encryptKey.getBytes(Constants.ENCODE);
            // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
            SecretKey secretKey = new SecretKeySpec(data, "HmacSHA1");
            // 生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance("HmacSHA1");
            // 用给定密钥初始化 Mac 对象
            mac.init(secretKey);
            byte[] text = encryptText.getBytes(Constants.ENCODE);
            byte[] textFinal = mac.doFinal(text);
            // 完成 Mac 操作, base64编码，将byte数组转换为字符串
            return new String(Base64.encodeBase64(textFinal), Constants.ENCODE);
        } catch (Exception e) {
            throw new RuntimeException("signWithhmacSHA1Encrypt fail", e);
        }
    }

}
