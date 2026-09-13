package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClientIpResolver {
    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );

    @Value(AppConstants.PropertyPlaceholders.SECURITY_PROXY_TRUSTED_PROXY_CIDRS)
    private List<String> trustedProxyCidrs;

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddress)) {
            return logAndReturn(request, remoteAddress);
        }

        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return logAndReturn(request, remoteAddress);
        }

        List<String> forwardedIps = Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .toList();

        for (int index = forwardedIps.size() - 1; index >= 0; index--) {
            String candidate = forwardedIps.get(index);
            if (parseIp(candidate) == null) {
                continue;
            }
            if (!isTrustedProxy(candidate)) {
                return logAndReturn(request, candidate);
            }
        }

        return logAndReturn(request, remoteAddress);
    }

    private String logAndReturn(HttpServletRequest request, String clientIp) {
        log.info(
                "IP DEBUG remoteAddr={}, xForwardedFor={}, xRealIp={}, resolved={}",
                request.getRemoteAddr(),
                request.getHeader(X_FORWARDED_FOR),
                request.getHeader(X_REAL_IP),
                clientIp
        );
        return clientIp;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        InetAddress address = parseIp(ip);
        if (address == null) {
            return false;
        }

        if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
            return true;
        }

        return trustedProxyCidrs != null && trustedProxyCidrs.stream()
                .map(String::trim)
                .filter(cidr -> !cidr.isBlank())
                .anyMatch(cidr -> matchesCidr(address, cidr));
    }

    private boolean matchesCidr(InetAddress address, String cidr) {
        String[] parts = cidr.split("/");
        try {
            InetAddress network = parseIp(parts[0].trim());
            if (network == null) {
                return false;
            }
            byte[] addressBytes = address.getAddress();
            byte[] networkBytes = network.getAddress();
            if (addressBytes.length != networkBytes.length) {
                return false;
            }

            int prefixLength = parts.length == 2
                    ? Integer.parseInt(parts[1].trim())
                    : addressBytes.length * 8;
            if (prefixLength < 0 || prefixLength > addressBytes.length * 8) {
                return false;
            }

            BigInteger addressValue = new BigInteger(1, addressBytes);
            BigInteger networkValue = new BigInteger(1, networkBytes);
            int shift = addressBytes.length * 8 - prefixLength;

            return addressValue.shiftRight(shift).equals(networkValue.shiftRight(shift));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private InetAddress parseIp(String ip) {
        if (ip == null || ip.isBlank() || !isIpLiteral(ip)) {
            return null;
        }

        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private boolean isIpLiteral(String ip) {
        return IPV4_PATTERN.matcher(ip).matches() || ip.contains(":");
    }
}
