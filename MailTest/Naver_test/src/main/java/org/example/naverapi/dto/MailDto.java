package org.example.naverapi.dto;

public class MailDto {
    private String from;
    private String subject;
    private String sentDate;

    public MailDto(String from, String subject, String sentDate) {
        this.from = from;
        this.subject = subject;
        this.sentDate = sentDate;
    }

    public String getFrom() { return from; }
    public String getSubject() { return subject; }
    public String getSentDate() { return sentDate; }
}