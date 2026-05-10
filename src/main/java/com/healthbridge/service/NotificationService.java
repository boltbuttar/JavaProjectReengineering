package com.healthbridge.service;

/**
 * NotificationService — demonstrates Lazy Class (Category 4).
 *
 * Smells:
 *   - Lazy Class: does almost nothing; all real notification logic is inlined
 *     directly in PatientManager instead of being here.
 *   - Dead Code: sendWhatsApp() and sendPush() are never called.
 *   - Speculative Generality: the class was created "for future use"
 *     but only contains stubs.
 */
public class NotificationService {

    // ---- Lazy Class: this class barely justifies its existence ----

    public void sendSMS(String phone, String message) {
        // Real implementation would call an SMS gateway
        System.out.println("[SMS] To: " + phone + " | Msg: " + message);
    }

    public void sendEmail(String email, String subject, String body) {
        // Real implementation would use JavaMail
        System.out.println("[EMAIL] To: " + email + " | Subject: " + subject);
    }

    // ---- Dead Code: never called anywhere in the system ----
    public void sendWhatsApp(String phone, String message) {
        System.out.println("[WHATSAPP] To: " + phone + " | Msg: " + message);
    }

    public void sendPushNotification(String deviceToken, String title, String body) {
        System.out.println("[PUSH] Token: " + deviceToken + " | Title: " + title);
    }

    /** @deprecated incomplete implementation */
    public void scheduleReminder(String phone, String message, String scheduleTime) {
        // Never implemented — speculative generality
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
