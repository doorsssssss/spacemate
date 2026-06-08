package com.spacemate.domain.entity;

/**
 * 客户端请求信息。
 *
 * @param ip 客户端 IP。
 * @param userAgent 客户端 User-Agent。
 */
public record ClientInfo(String ip, String userAgent) {
}
