package org.example.naverapi.service;

import jakarta.mail.*;
import org.example.naverapi.dto.MailDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Service
public class NaverMailService {

    private final Session session;

    public NaverMailService(Session session) {
        this.session = session;
    }

    public List<MailDto> fetchEmails() {
        Store store = null;
        Folder inbox = null;
        List<MailDto> MailList = new ArrayList<>();

        try {
            store = session.getStore("imaps");
            store.connect();

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            int totalMessages = messages.length;
            int start = Math.max(0, totalMessages - 5);

            System.out.println("[+] 메일 총 개수: " + totalMessages);
            System.out.println("[+] 최신 5개 메일을 가져옵니다...");

            for (int i = totalMessages - 1; i >= start; i--) {
                Message message = messages[i];
                MailList.add(new MailDto(
                        message.getFrom()[0].toString(),
                        message.getSubject(),
                        message.getSentDate().toString()
                ));
            }
            return MailList;

        } catch (MessagingException e) {
            System.err.println("메일 처리 중 오류가 발생했습니다.");
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                System.err.println("리소스 정리 중 오류가 발생했습니다.");
                e.printStackTrace();
            }
        }
    }
}
