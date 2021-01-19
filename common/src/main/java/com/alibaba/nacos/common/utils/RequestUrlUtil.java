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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

/**
 * In order to prevent the situation of overstepping authority,
 * it is necessary to do URL rule matching after some normalization
 * processing of the URL.
 * <p>ref：UrlPathHelper.getLookupPathForRequest</p>
 * <p>ref：RequestUtil.normalize</p>
 *
 * @author vince
 */
public class RequestUrlUtil {

    /**
     * uri decode.
     * @param uri uri
     * @param charset charset
     * @return decoded uri
     */
    public static String uriDecode(String uri, Charset charset) {
        try {
            charset = charset == null ? StandardCharsets.UTF_8 : charset;
            return uriDecode0(uri, charset);
        } catch (UnsupportedCharsetException ex) {
            try {
                return URLDecoder.decode(uri, charset.name());
            } catch (UnsupportedEncodingException e) {
                return uri;
            }
        }
    }

    /**
     * uri decode method.
     * @param source string that will be decoded
     * @param charset string charset
     * @return decoded uri
     */
    private static String uriDecode0(String source, Charset charset) {
        int length = source.length();
        if (length == 0) {
            return source;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
            boolean changed = false;

            for (int i = 0; i < length; ++i) {
                int ch = source.charAt(i);
                if (ch == '%') {
                    if (i + 2 >= length) {
                        throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }

                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }

                    baos.write((char) ((u << 4) + l));
                    i += 2;
                    changed = true;
                } else {
                    baos.write(ch);
                }
            }

            //noinspection StringOperationCanBeSimplified
            return changed ? new String(baos.toByteArray(), charset) : source;
        }
    }

    /**
     * normalize uri.
     * @param path uri
     * @param replaceBackSlash if replace back slash
     * @return normalized uri
     */
    public static String normalize(String path, boolean replaceBackSlash) {
        if (path == null) {
            return null;
        }
        String normalized = path;
        if (replaceBackSlash && path.indexOf(92) >= 0) {
            normalized = path.replace('\\', '/');
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        boolean addedTrailingSlash = false;
        if (normalized.endsWith("/.") || normalized.endsWith("/..")) {
            normalized = normalized + "/";
        }
        if (normalized.endsWith("/")) {
            addedTrailingSlash = true;
        }

        while (true) {
            int index = normalized.indexOf("//");
            if (index == -1) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }

        while (true) {
            int index = normalized.indexOf("/./");
            if (index == -1) {
                break;
            }
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }

        while (true) {
            int index = normalized.indexOf("/../");
            if (index == -1) {
                break;
            }
            if (index == 0) {
                return null;
            }
            int index2 = normalized.lastIndexOf(47, index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }

        if (normalized.length() > 1 && addedTrailingSlash) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * normalize uri.
     * @param path uri path
     * @return normalized uri
     */
    public static String normalize(String path) {
        return normalize(path, true);
    }

    /**
     * resolve uri that with semi colon.
     * @param uri uri
     * @return resolved uri
     */
    public static String stripPathParams(String uri) {
        if (uri.indexOf(59) == -1) { // 包含 ;
            return uri;
        }
        StringBuilder sb = new StringBuilder(uri.length());
        int pos = 0;
        int limit = uri.length();

        while (pos < limit) {
            int nextSemiColon = uri.indexOf(59, pos);
            if (nextSemiColon == -1) {
                nextSemiColon = limit;
            }

            sb.append(uri, pos, nextSemiColon);
            int followingSlash = uri.indexOf(47, nextSemiColon);
            if (followingSlash < 0) {
                pos = limit;
            } else {
                pos = followingSlash;
            }
        }

        return sb.toString();
    }

    /**
     * same origin judge, for prevent cors.
     * @param scheme scheme
     * @param host address host
     * @param port address pot
     * @param origin allow origin
     * @return is same origin
     */
    public static boolean isSameOrigin(String scheme, String host, int port, String origin) {
        StringBuilder target = new StringBuilder();
        if (scheme == null) {
            return false;
        } else {
            scheme = scheme.toLowerCase(Locale.ENGLISH);
            target.append(scheme);
            target.append("://");
            if (host == null) {
                return false;
            } else {
                target.append(host);
                if (target.length() == origin.length()) {
                    if (("http".equals(scheme) || "ws".equals(scheme)) && port != 80
                            || ("https".equals(scheme) || "wss".equals(scheme)) && port != 443) {
                        target.append(':');
                        target.append(port);
                    }
                } else {
                    target.append(':');
                    target.append(port);
                }

                return origin.equals(target.toString());
            }
        }
    }

    /**
     * is valid origin.
     * @param origin allow origin
     * @return yes or not
     */
    public static boolean isValidOrigin(String origin) {
        if (origin.contains("%")) {
            return false;
        } else if ("null".equals(origin)) {
            return true;
        } else if (origin.startsWith("file://")) {
            return true;
        } else {
            URI originUri;
            try {
                originUri = new URI(origin);
            } catch (URISyntaxException e) {
                return false;
            }

            return originUri.getScheme() != null;
        }
    }
}

