/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.common.utils;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.common.codec.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * spas encode.
 *
 * @author vince
 */
public class SpasAdapter {

    /**
     * calculate sign.
     * @param tenant tenent
     * @param group group
     * @param timestamp timestamp
     * @param secretKey secret key
     * @return signature
     */
    public static String calculateSign(String tenant, String group, String timestamp, String secretKey) {

        String resource = "";
        if (tenant != null && group != null) {
            resource += tenant + "+" + group;
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
