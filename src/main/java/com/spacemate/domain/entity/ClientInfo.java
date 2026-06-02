package com.spacemate.domain.entity;

/**
 * 瀹㈡埛绔俊鎭€?
 * <p>
 * 璁板綍瀹㈡埛绔?IP 涓?User-Agent锛岀敤浜庣櫥褰曞璁°€侀鎺т笌娲诲姩璁板綍銆?
 * 璇ュ璞￠€氬父鐢辨帶鍒跺櫒浠?HTTP 璇锋眰涓В鏋愮敓鎴愩€?
 *
 * @param ip        瀹㈡埛绔?IP 鍦板潃锛堝彲鑳芥潵鑷?`X-Forwarded-For` 鎴栬繙绔湴鍧€锛夈€?
 * @param userAgent 瀹㈡埛绔?User-Agent 瀛楃涓层€?
 */
public record ClientInfo(String ip, String userAgent) {
}

