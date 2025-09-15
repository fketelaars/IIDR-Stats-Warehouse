package com.ibm.replication.iidr.utils;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

public class MailUtil {
	
	 public static void sendEmail(Settings settings, String subject, String body) throws AddressException, MessagingException {
	        // Extract email configuration using Settings class
	        String host = settings.getString("mail.smtp.host");
	        String port = settings.getString("mail.smtp.port");
	        String auth = settings.getString("mail.smtp.auth");
	        String starttls = settings.getString("mail.smtp.starttls.enable");
	        String senderEmail = settings.getString("mail.sender.email");
	        String senderPassword = settings.getEncryptedString("mail.sender.password");
	        String recipientEmail = settings.getString("mail.recipient.email");

	        // Configure mail session
	        Properties sessionProps = new Properties();
	        sessionProps.put("mail.smtp.host", host);
	        sessionProps.put("mail.smtp.port", port);
	        sessionProps.put("mail.smtp.auth", auth);
	        sessionProps.put("mail.smtp.starttls.enable", starttls);


	        Session session = Session.getInstance(sessionProps, new Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(senderEmail, senderPassword);
	            }
    });

    // Create and send email
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(senderEmail));
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
    message.setSubject(subject);
    message.setText(body);

    Transport.send(message);
}
}
