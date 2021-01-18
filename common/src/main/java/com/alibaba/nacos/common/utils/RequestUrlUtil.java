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
 * 为了防止出现越权的情况，需要对url进行一些规范化处理后，才做url规则匹配
 * <p>参考1：UrlPathHelper.getLookupPathForRequest</p>
 * <p>参考2：RequestUtil.normalize</p>
 */
public class RequestUrlUtil {

    public static String uriDecode(String uri, Charset charset) {
        try {
            charset = charset == null ? StandardCharsets.UTF_8 : charset;
            return uriDecode0(uri, charset);
        }
        catch (UnsupportedCharsetException ex) {
            try {
                return URLDecoder.decode(uri, charset.name());
            } catch (UnsupportedEncodingException e) {
                return uri;
            }
        }
    }

    private static String uriDecode0(String source, Charset charset) {
        int length = source.length();
        if (length == 0) {
            return source;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
            boolean changed = false;

            for(int i = 0; i < length; ++i) {
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

                    baos.write((char)((u << 4) + l));
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

    public static String normalize(String path) {
        return normalize(path, true);
    }

    public static String stripPathParams(String uri) {
        if (uri.indexOf(59) == -1) { // 包含 ;
            return uri;
        }
        StringBuilder sb = new StringBuilder(uri.length());
        int pos = 0;
        int limit = uri.length();

        while(pos < limit) {
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
                    if (("http".equals(scheme) || "ws".equals(scheme)) && port != 80 || ("https".equals(scheme) || "wss".equals(scheme)) && port != 443) {
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

    public static boolean isValidOrigin(String origin) {
        if (origin.contains("%")) {
            return false;
        } else if ("null".equals(origin)) {
            return true;
        } else if (origin.startsWith("file://")) {
            return true;
        } else {
            URI originURI;
            try {
                originURI = new URI(origin);
            } catch (URISyntaxException var3) {
                return false;
            }

            return originURI.getScheme() != null;
        }
    }
}

