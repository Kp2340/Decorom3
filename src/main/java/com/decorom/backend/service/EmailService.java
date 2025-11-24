package com.decorom.backend.service;

import com.decorom.backend.entity.Order;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOrderAlert(Order order) {
        String subject = String.format("[Order #%s] - Status: %s - IP: %s",
                order.getId(), order.getPaymentStatus(), order.getUserIP());

        String body = String.format("""
                <html>
                <body>
                    <h2>Order Alert</h2>
                    <table border='1'>
                        <tr>
                            <th>Field</th>
                            <th>Value</th>
                        </tr>
                        <tr>
                            <td>Order ID</td>
                            <td>%s</td>
                        </tr>
                        <tr>
                            <td>Frontend Price</td>
                            <td>₹%.2f</td>
                        </tr>
                        <tr>
                            <td>Server Price</td>
                            <td>₹%.2f</td>
                        </tr>
                        <tr>
                            <td>Price Valid</td>
                            <td>%s</td>
                        </tr>
                        <tr>
                            <td>IP Address</td>
                            <td>%s</td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                order.getId(),
                order.getFrontendPrice(),
                order.getServerCalculatedPrice(),
                order.isPriceValid() ? "YES" : "NO - POTENTIAL FRAUD",
                order.getUserIP());

        sendEmail("kushpatel2354@gmail.com", subject, body);
    }

    public void sendPaymentConfirmation(Order order) {
        String subject = "Payment Received - Order #" + order.getId();
        String body = "<h1>Payment Success</h1><p>The payment for order " + order.getId()
                + " has been successfully verified.</p>";
        sendEmail("kushpatel2354@gmail.com", subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace(); // Log this properly in production
        }
    }
}
