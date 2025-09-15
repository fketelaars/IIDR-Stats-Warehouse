package com.ibm.replication.iidr.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ibm.replication.iidr.warehouse.logging.LogDatabase.SubAlert;

public class SubscriptionAlert {

	static Logger logger;
	
    private static final SubscriptionAlert INSTANCE = new SubscriptionAlert();

	public final Map<String, List<SubAlert>> pendingAlertsByDS = new ConcurrentHashMap<String, List<SubAlert>>();
     
	private SubscriptionAlert() {
	
       logger= LogManager.getLogger();

	}

    public static SubscriptionAlert getInstance() {
        return INSTANCE;
    }
	// Give getter if datastore received sent those respective alerts
	public List<SubAlert> getAlertsForDatastore(String datastore) {
		return pendingAlertsByDS.get(datastore);
	}

	// Remove the alerts once sent
	public void removeAlertsForDatastore(String datastore) {
		pendingAlertsByDS.remove(datastore);
	}
	
	public Map<String, List<SubAlert>> snapshotAndClear() {
        Map<String, List<SubAlert>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<SubAlert>> e : pendingAlertsByDS.entrySet()) {
            // copy each list to avoid mutation during send
        	logger.debug("Inside snapshots and clear");
        	if(e.getValue() != null && !e.getValue().isEmpty()) {
        		snapshot.put(e.getKey(), new ArrayList<>(e.getValue()));
        	}
            
        }
        // clear after copying
        pendingAlertsByDS.clear();
        return snapshot;
    }
}
