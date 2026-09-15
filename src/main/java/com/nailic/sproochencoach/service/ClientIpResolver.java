package com.nailic.sproochencoach.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Service
public class ClientIpResolver {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final List<String> DEFAULT_TRUSTED_PROXY_CIDRS = List.of(
            "127.0.0.1/32",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16"
    );

    @Value("#{'${security.trusted-proxy-cidrs:127.0.0.1/32,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}'.split(',')}")
    private List<String> trustedProxyCidrs = DEFAULT_TRUSTED_PROXY_CIDRS;

    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        String remoteAddress = request.getRemoteAddr();
        if (forwardedFor == null || forwardedFor.isBlank() || !isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }

        List<String> forwardedAddresses = Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .filter(address -> !address.isBlank())
                .toList();
        if (forwardedAddresses.isEmpty()) {
            return remoteAddress;
        }

        for (int index = forwardedAddresses.size() - 1; index >= 0; index--) {
            String forwardedAddress = forwardedAddresses.get(index);
            if (!isTrustedProxy(forwardedAddress)) {
                return forwardedAddress;
            }
        }

        return forwardedAddresses.get(0);
    }

    private boolean isTrustedProxy(String address) {
        List<String> effectiveTrustedProxyCidrs = trustedProxyCidrs.isEmpty()
                ? DEFAULT_TRUSTED_PROXY_CIDRS
                : trustedProxyCidrs;
        return effectiveTrustedProxyCidrs.stream()
                .map(String::trim)
                .filter(cidr -> !cidr.isBlank())
                .anyMatch(cidr -> isInCidr(address, cidr));
    }

    private boolean isInCidr(String address, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress inetAddress = InetAddress.getByName(address);
            InetAddress networkAddress = InetAddress.getByName(parts[0]);
            byte[] addressBytes = inetAddress.getAddress();
            byte[] networkBytes = networkAddress.getAddress();
            if (addressBytes.length != networkBytes.length) {
                return false;
            }

            int prefixLength = parts.length == 1 ? addressBytes.length * 8 : Integer.parseInt(parts[1]);
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int index = 0; index < fullBytes; index++) {
                if (addressBytes[index] != networkBytes[index]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = (-1) << (8 - remainingBits);
            return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            return false;
        }
    }
}
