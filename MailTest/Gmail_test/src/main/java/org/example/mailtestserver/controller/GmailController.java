package org.example.mailtestserver.controller;

import org.example.mailtestserver.dto.AttachmentDto;
import org.example.mailtestserver.dto.EmailDto;
import org.example.mailtestserver.service.GmailService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@RequestMapping("/")
public class GmailController {

    private final AuthorizationCodeFlow flow; //Google OAuth 2.0 인증 관리 객체
    private final GmailService gmailService; //Gmail API 관련 로직 처리하는 서비스
    private final String redirectUri; //OAuth 2.0 인증 성공후 redirection될 URI

    private static final String USER_ID = "me"; //사용자 ID 임의배정
    private static final String CREDENTIAL_SESSION_KEY = "google_credential"; //세션에 credential 객체 저장하기 위한 키

    @Autowired
    public GmailController(
            AuthorizationCodeFlow flow,
            GmailService gmailService,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.flow = flow;
        this.gmailService = gmailService;
        this.redirectUri = redirectUri;
    }

    //google Oauth2 로그인 - google 인증 페이지로 이동
    @GetMapping("/login/google")
    public void login(HttpServletResponse response) throws IOException {
        String url = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
        response.sendRedirect(url);
    }

    /*
    * google 로그인 성공 후 인증 코드 받음
    * code: Google로부터 받은 인증 코드
    */
    @GetMapping("/login/oauth2/code/google")
    public void oauthCallback(@RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, USER_ID);

        request.getSession().setAttribute(CREDENTIAL_SESSION_KEY, credential); //session에 인증받은 credential 저장
        response.sendRedirect("/"); //인증 완료 후 메인 페이지('/')로 redirection
    }

    /*
    * 이메일 조회
    * session : credential 정보 가져오기 위한 세션
    * EmailDTO : 이메일 데이터 파싱위해 만들어 놓은 Data Transfer Object(객체)
    */
    @GetMapping("/api/latest-email")
    public ResponseEntity<EmailDto> getLatestEmail(HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            EmailDto email = gmailService.getLatestEmail(credential);
            if (email == null) { //email 없으면 notFound 응답 : 에러코드 404
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(email);
        } catch (IOException e) {
            // 서버 내부 오류 발생 시 : 에러코드 500
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); //EmailDTO를 담은 responseEntity을 return 한다.
        }
    }

    //특정 이메일의 첨부파일 목록을 조회하는 API - 첨부파일 여러개 일 수 있으므로 목록.
    @GetMapping("/api/attachments/{messageId}")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsList(
            @PathVariable("messageId") String messageId,
            HttpSession session) {

        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<AttachmentDto> attachments = gmailService.listAttachments(credential, messageId);
            return ResponseEntity.ok(attachments);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //첨부파일 다운로드 API
    @GetMapping("/api/attachment/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @RequestParam("msgId") String messageId, //첨부파일의 메일 ID
            @RequestParam("attId") String attachmentId, //첨부파일의 ID
            @RequestParam("filename") String filename, //파일명(다운로드시 저장명)
            HttpSession session) { //session에 저장된 credential 정보를 가져오기 위한 세션

        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            //
            byte[] fileBytes = gmailService.getAttachmentBytes(credential, messageId, attachmentId);

            //다국어 파일명을 지원하기 위해 파일명 인코딩
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .body(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

     //세션에서 Credential을 가져오고, 필요 시 토큰을 갱신하는 헬퍼 메서드
    private Credential getRefreshedCredential(HttpSession session) { //session은 현재 HttpSession
        Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
        if (credential == null) {
            return null;
        }
        try { //access token 만료 시간이 60초 이내 남았다면 갱신
            if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
                if (!credential.refreshToken()) {
                    return null; // 리프레시 실패 null 반환
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return credential;
    }
}
