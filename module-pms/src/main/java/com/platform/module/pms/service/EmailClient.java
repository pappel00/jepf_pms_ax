package com.platform.module.pms.service;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * 알림 이메일 발송(SMTP). 서버·계정은 설정(pms.notify.email.*, 환경변수 PMS_SMTP_*)에서 읽는다.
 * 스프링 기본 메일 자동 구성(spring.mail.*)은 쓰지 않고 여기서 직접 만들어, 설정이 없으면 이메일 채널만 「건너뜀」이 된다.
 * 텍스트 메일만 보낸다(버튼이 없으므로 진척 입력은 PMS 화면에서 한다).
 */
@Component
public class EmailClient {

    /** 발송 실패(SMTP 연결·인증·전송 오류). */
    public static class EmailException extends RuntimeException {
        public EmailException(String message) {
            super(message);
        }
    }

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean ssl;
    private JavaMailSenderImpl sender;

    public EmailClient(@Value("${pms.notify.email.host:}") String host,
                       @Value("${pms.notify.email.port:465}") int port,
                       @Value("${pms.notify.email.username:}") String username,
                       @Value("${pms.notify.email.password:}") String password,
                       @Value("${pms.notify.email.from:}") String from,
                       @Value("${pms.notify.email.ssl:true}") boolean ssl) {
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.from = from == null || from.isBlank() ? this.username : from.trim();
        this.ssl = ssl;
    }

    /** SMTP 서버·계정이 설정되어 있는지. */
    public boolean isConfigured() {
        return !host.isEmpty() && !username.isEmpty() && !password.isEmpty();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFrom() {
        return from;
    }

    /** SMTP 서버에 접속·인증이 되는지 확인한다(메일은 보내지 않는다). */
    public void testConnection() {
        try {
            sender().testConnection();
        } catch (jakarta.mail.MessagingException | RuntimeException e) {
            throw new EmailException("SMTP 접속에 실패했습니다: " + e.getMessage());
        }
    }

    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender().send(msg);
        } catch (RuntimeException e) {
            throw new EmailException("메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    private synchronized JavaMailSenderImpl sender() {
        if (!isConfigured()) {
            throw new EmailException("SMTP 설정이 없습니다(PMS_SMTP_HOST·USERNAME·PASSWORD).");
        }
        if (sender == null) {
            JavaMailSenderImpl s = new JavaMailSenderImpl();
            s.setHost(host);
            s.setPort(port);
            s.setUsername(username);
            s.setPassword(password);
            s.setDefaultEncoding("UTF-8");
            Properties p = s.getJavaMailProperties();
            p.put("mail.smtp.auth", "true");
            // 465 는 처음부터 SSL, 587 등은 STARTTLS
            if (ssl) {
                p.put("mail.smtp.ssl.enable", "true");
            } else {
                p.put("mail.smtp.starttls.enable", "true");
            }
            p.put("mail.smtp.connectiontimeout", "8000");
            p.put("mail.smtp.timeout", "15000");
            p.put("mail.smtp.writetimeout", "15000");
            sender = s;
        }
        return sender;
    }
}
