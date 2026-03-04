package com.esprit.microservice.pidev.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) throws MessagingException {
        String resetLink = "http://localhost:4200/#/reset-password?token=" + resetToken;

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: 'Segoe UI', sans-serif; background: #f8f9fe; margin: 0; padding: 0; }
                .wrapper { max-width: 520px; margin: 40px auto; background: #fff;
                           border-radius: 20px; overflow: hidden;
                           box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
                .header  { background: linear-gradient(135deg, #9c27b0, #00acc1);
                           padding: 36px 32px; text-align: center; }
                .header h1 { color: #fff; margin: 0; font-size: 26px; font-weight: 800;
                             letter-spacing: -0.5px; }
                .header p  { color: rgba(255,255,255,0.85); margin: 6px 0 0;
                             font-size: 14px; }
                .body    { padding: 36px 32px; }
                .body p  { color: #555; font-size: 14px; line-height: 1.7; margin: 0 0 16px; }
                .btn-wrap { text-align: center; margin: 28px 0; }
                .btn     { display: inline-block; padding: 14px 40px;
                           background: linear-gradient(135deg, #9c27b0, #7b1fa2);
                           color: #fff !important; text-decoration: none;
                           border-radius: 50px; font-size: 15px; font-weight: 700;
                           box-shadow: 0 4px 14px rgba(156,39,176,0.4); }
                .note    { font-size: 12px !important; color: #aaa !important; }
                .footer  { background: #f8f9fe; padding: 20px 32px; text-align: center;
                           font-size: 12px; color: #bbb; }
                .link-fallback { word-break: break-all; color: #9c27b0;
                                 font-size: 12px; }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>🔐 Prolance</h1>
                  <p>Password Reset Request</p>
                </div>
                <div class="body">
                  <p>Hi there,</p>
                  <p>We received a request to reset the password for your Prolance account.
                     Click the button below to choose a new password:</p>
                  <div class="btn-wrap">
                    <a href="%s" class="btn">Reset My Password</a>
                  </div>
                  <p class="note">⏰ This link expires in <strong>30 minutes</strong>.
                     If you didn't request a password reset, you can safely ignore this email
                     — your account will remain secure.</p>
                  <p class="note">Or copy this link into your browser:</p>
                  <p class="link-fallback">%s</p>
                </div>
                <div class="footer">
                  &copy; %d Prolance · All rights reserved
                </div>
              </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, java.time.Year.now().getValue());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Reset your Prolance password");
        helper.setText(html, true); // true = HTML
        helper.setFrom("noreply@prolance.com");

        mailSender.send(message);
    }
}