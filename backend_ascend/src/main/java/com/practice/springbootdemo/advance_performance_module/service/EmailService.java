package com.practice.springbootdemo.advance_performance_module.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.from-name:ASCEND Performance Suite}")
    private String fromName;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a 6-digit password reset OTP to the user's registered email address.
     * If SMTP is unconfigured or fails, gracefully logs the OTP for dev testing.
     */
    public void sendPasswordResetOtp(String toEmail, String recipientName, String otp) {
        log.info("Initiating password reset OTP email delivery to: '{}'", toEmail);

        String displayName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "ASCEND User";
        String subject = "🔒 ASCEND Performance - Your Password Reset OTP";
        String htmlContent = buildOtpHtmlTemplate(displayName, otp);

        if (mailSender != null && fromEmail != null && !fromEmail.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, fromName);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Password reset OTP email successfully sent via SMTP to: '{}'", toEmail);
                return;
            } catch (Exception ex) {
                log.warn("SMTP email dispatch failed for '{}': {}. Falling back to system logging.", toEmail, ex.getMessage());
            }
        } else {
            log.info("SMTP credentials not fully configured (spring.mail.username). Delivering OTP via console log.");
        }

        // Always log OTP for development convenience
        log.info("===================================================================");
        log.info("📧 [ASCEND EMAIL SERVICE - PASSWORD RESET OTP]");
        log.info("   To:      {}", toEmail);
        log.info("   User:    {}", displayName);
        log.info("   OTP:     {}", otp);
        log.info("   Expires: In 15 minutes");
        log.info("===================================================================");
    }

    private String buildOtpHtmlTemplate(String name, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
                .card { max-width: 520px; margin: 0 auto; background: #1e293b; border-radius: 16px; border: 1px solid #334155; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }
                .header { text-align: center; border-bottom: 1px solid #334155; padding-bottom: 20px; margin-bottom: 24px; }
                .brand { font-size: 24px; font-weight: 800; color: #818cf8; letter-spacing: 1px; }
                .sub { font-size: 13px; color: #94a3b8; margin-top: 4px; }
                .otp-box { background: #0f172a; border: 2px dashed #6366f1; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0; }
                .otp-code { font-size: 32px; font-family: monospace; font-weight: 900; letter-spacing: 8px; color: #38bdf8; }
                .warning { font-size: 12px; color: #fbbf24; background: rgba(251, 191, 36, 0.1); border-radius: 8px; padding: 10px; margin-top: 20px; border-left: 3px solid #f59e0b; }
                .footer { font-size: 11px; color: #64748b; text-align: center; margin-top: 30px; border-top: 1px solid #334155; padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="header">
                  <div class="brand">ASCEND PERFORMANCE</div>
                  <div class="sub">Corporate Identity & Security Services</div>
                </div>
                <p>Hello <strong>%s</strong>,</p>
                <p style="color: #cbd5e1; font-size: 14px; line-height: 1.6;">
                  We received a request to reset the password for your ASCEND Performance Management account.
                  Please use the following 6-digit One-Time Password (OTP) to complete your verification:
                </p>
                <div class="otp-box">
                  <div class="otp-code">%s</div>
                </div>
                <div class="warning">
                  ⏱️ <strong>Note:</strong> This verification OTP is valid for <strong>15 minutes</strong> only. If you did not request this password reset, please contact your HR administrator immediately.
                </div>
                <div class="footer">
                  © 2026 ASCEND Performance Suite • Enterprise HR & OKR Governance
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, otp);
    }
}
