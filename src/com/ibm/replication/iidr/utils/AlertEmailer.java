package com.ibm.replication.iidr.utils;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.replication.iidr.warehouse.logging.LogDatabase;

public class AlertEmailer {
	
	private Settings settings;
	static Logger logger;

    // Constructor that accepts Settings as an argument
    public AlertEmailer(Settings settings) {
        this.settings = settings;
		logger =LogManager.getLogger();
    }
	
	 // Method to send email for datastore
    public void sendEmailForDatastore(String dataStore, List<LogDatabase.SubAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            logger.info("No failed/inactive subscriptions for datastore " + dataStore + " in this cycle.");
            return;
        }

        String subject = "[CDC] " + dataStore + " subscriptions with issues (" + alerts.size() + ")";
        StringBuilder body = new StringBuilder();
        body.append("Datastore: ").append(dataStore).append('\n');
        body.append("Collected at: ").append(new Timestamp(System.currentTimeMillis())).append("\n\n");
        body.append("Subscription                           State       Last Seen (COLLECT_TS)\n");
        body.append("-------------------------------------------------------------------------------\n");

        for (LogDatabase.SubAlert alert : alerts) {
            body.append(String.format("%-40s %-12s %s%n", alert.subscription, alert.state, alert.ts));
        }

        // Send the email using an existing utility (MailUtil or custom method)
        try {
			MailUtil.sendEmail(settings, subject, body.toString());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			logger.error("Failed to send email notification: " + e.getMessage());		}
    }

 // NEW: One email containing all datastores and their alerts
    public void sendConsolidatedEmail(Map<String, List<LogDatabase.SubAlert>> alertsByDatastore) {
    	logger.debug("Preparing sendConsolidatedEmail");
        if (alertsByDatastore == null || alertsByDatastore.isEmpty()) {
            logger.info("No failed/inactive subscriptions across all datastores in this cycle.");
            return;
        }

        // Count totals and sort datastores for a stable, readable output
        List<String> datastores = new ArrayList<>(alertsByDatastore.keySet());
        Collections.sort(datastores);

        int totalAlerts = 0;
        for (String ds : datastores) {
            List<LogDatabase.SubAlert> list = alertsByDatastore.get(ds);
            if (list != null) totalAlerts += list.size();
        }

        String subject = "[CDC] Subscriptions with issues across datastores (" + totalAlerts + " alerts, " +
                         datastores.size() + " datastores)";

        StringBuilder body = new StringBuilder();
        body.append("Collected at: ").append(new Timestamp(System.currentTimeMillis())).append("\n\n");

        for (String ds : datastores) {
            List<LogDatabase.SubAlert> alerts = alertsByDatastore.get(ds);
            if (alerts == null || alerts.isEmpty()) continue;

            body.append("Datastore: ").append(ds).append("  (").append(alerts.size()).append(" issues)\n\n");
            body.append("Subscription                           State       Last Seen (COLLECT_TS)\n");
            body.append("-------------------------------------------------------------------------------\n");
            // Optional: sort alerts by time (latest first)
            alerts.sort((a, b) -> b.ts.compareTo(a.ts));
            for (LogDatabase.SubAlert alert : alerts) {
                body.append(String.format("%-40s %-12s %s%n", alert.subscription, alert.state, alert.ts));
            }
            body.append("\n");
        }

        try {
            MailUtil.sendEmail(settings, subject, body.toString());
            logger.info("Consolidated alert email sent. Total alerts: " + totalAlerts + ", datastores: " + datastores.size());
        } catch (MessagingException e) {
            logger.error("Failed to send consolidated email notification: " + e.getMessage());
        }
    }
}
