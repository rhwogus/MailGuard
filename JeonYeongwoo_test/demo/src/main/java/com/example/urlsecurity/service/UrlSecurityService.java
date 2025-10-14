// ============================================
// 3. src/main/java/com/example/urlsecurity/service/UrlSecurityService.java
// ============================================
package com.example.urlsecurity.service;

import com.example.urlsecurity.model.UrlValidationResult;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlSecurityService {
    
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
        "javascript:", "data:", "vbscript:", "file:",
        "<script", "onerror=", "onclick=", "onload="
    );
    
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
        "example.com", "trusted-site.com", "naver.com", "google.com"
    );
    
    // 여기에 이상한 도메인들 리스트업 하기.
    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
        "malicious.com", "phishing-site.com"
    );
    
    public boolean isSafeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }
        
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            
            if (host == null) {
                return false;
            }
            
            for (String blocked : BLOCKED_DOMAINS) {
                if (host.endsWith(blocked)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    public UrlValidationResult validateUrl(String url) {
        UrlValidationResult result = new UrlValidationResult();
        result.setOriginalUrl(url);
        
        if (url == null || url.trim().isEmpty()) {
            result.setSafe(false);
            result.setReason("빈 URL입니다");
            return result;
        }
        
        String lowerUrl = url.toLowerCase();
        
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                result.setSafe(false);
                result.setReason("위험한 패턴 감지: " + pattern);
                return result;
            }
        }
        
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            result.setHost(host);
            
            if (host == null) {
                result.setSafe(false);
                result.setReason("유효하지 않은 호스트");
                return result;
            }
            
            for (String blocked : BLOCKED_DOMAINS) {
                if (host.endsWith(blocked)) {
                    result.setSafe(false);
                    result.setReason("차단된 도메인: " + blocked);
                    return result;
                }
            }
            
            result.setSafe(true);
            result.setReason("안전한 URL");
            
        } catch (URISyntaxException e) {
            result.setSafe(false);
            result.setReason("잘못된 URL 형식");
        }
        
        return result;
    }
}