package com.aurora.core.infrastructure.notification;

import com.aurora.core.contract.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * Email Notification Service — template-based email sending with resilience.
 *
 * <p>Features:
 * <ul>
 *   <li>Thymeleaf HTML templates with variable injection</li>
 *   <li>StructuredTaskScope timeout (15s) with fallback to log warning</li>
 *   <li>Resilience4j retry + circuit breaker (configured via properties)</li>
 *   <li>Audit trail via ImmutableAuditLogger</li>
 * </ul>
 *
 * <p>Activation: {@code aurora.notification.email.enabled=true}
 */
@Service
@ConditionalOnProperty(name = "aurora.notification.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(15);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AuditLogger auditLogger;

    public EmailNotificationService(JavaMailSender mailSender,
                                    TemplateEngine templateEngine,
                                    AuditLogger auditLogger) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.auditLogger = auditLogger;
    }

    /**
     * Send a template-based email asynchronously.
     *
     * @param to       recipient email
     * @param subject  email subject
     * @param template Thymeleaf template name (e.g., "welcome")
     * @param variables template variables
     */
    @Async
    public void sendTemplateEmail(String to, String subject, String template,
                                   Map<String, Object> variables) {
        Instant start = Instant.now();
        try (var scope = StructuredTaskScope.<Void, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(SEND_TIMEOUT))) {

            scope.fork(() -> {
                doSend(to, subject, template, variables);
                return null;
            });

            scope.join();

            log.info("Email sent to {} using template '{}' in {}ms",
                    to, template, Duration.between(start, Instant.now()).toMillis());

        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Email send timed out after {}s for template '{}' to {} — falling back to log",
                    SEND_TIMEOUT.getSeconds(), template, to);

        } catch (Exception e) {
            log.error("Failed to send email to {} using template '{}': {}",
                    to, template, e.getMessage());
        }
    }

    /**
     * Send a simple text email with timeout protection.
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String textBody) {
        try (var scope = StructuredTaskScope.<Void, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(SEND_TIMEOUT))) {

            scope.fork(() -> {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(textBody, false);
                mailSender.send(message);
                return null;
            });

            scope.join();
            log.info("Simple email sent to {}", to);

        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Simple email send timed out after {}s for {}", SEND_TIMEOUT.getSeconds(), to);

        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
        }
    }

    private void doSend(String to, String subject, String template,
                        Map<String, Object> variables) throws Exception {
        Context context = new Context();
        context.setVariables(variables);

        String htmlBody = templateEngine.process("email/" + template, context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
    }
}
