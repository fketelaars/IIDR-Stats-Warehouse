# Reference

The CDC Stats Warehouse utility collects the following types of information:
- Subscription state
- Subscription and datastore metrics
- Subscription and datastore event logs

For the subscription state and metrics, every record has a recorded time stamp, which is determined at the beginning of the collection cycle. All subscription state and metrics recording which are done every interval have the same collection time stamp associated with them.

## Subscription state
For subscriptions, the activity state is logged on every iteration. Effectively, the following data elements are recorded to the CDC\_SUB\_STATUS table, or the SubStatus CSV files:
- Source datastore name - Name of the source datastore of the subscription
- Subscription name - Name of the subscription within the source datastore
- Collect time stamp - Timestamp when the subscription state was collected
- Subscription state - State of the subscription (Inactive, Mirror Continuous, ...)

The database table CDC\_SUB\_STATUS has the following columns to represent the data elements:
- SRC_DATASTORE
- SUBSCRIPTION
- COLLECT_TS
- SUBSCRIPTION_STATUS

The SRC\_DATASTORE, SUBSCRIPTION and COLLECT\_TS make up the primary key of the CDC\_SUB\_STATUS table.

If you choose to log the subscription state into flat files, the column layout of the flat file will be as follows:
dataStore|subscriptionName|collectTimestamp|subscriptionState

## Subscription and datastore metrics
If a subscription is actively replicating, the metrics of the subscription, as well as the source and target datastores, are collected. The data elements that are recorded into the CDC\_STATS\_ALL table, or the Statistics CSV files are:
- Source datastore name - Name of the source datastore of the subscription
- Subscription name - Name of the subscription within the source datastore
- Collect time stamp - Timestamp when the subscription state was collected
- Source or target indicator - Indicates whether the metric applies to the source or the target
- Metric ID - The identification of the metric that is recorded
- Metric Value - The value of the recorded metric

The database table CDC\_STATS\_ALL has the following structure:
- SRC_DATASTORE
- SUBSCRIPTION
- COLLECT_TS
- SRC_TGT
- METRIC_ID
- METRIC_VALUE

The SRC\_DATASTORE, SUBSCRIPTION, COLLECT\_TS and SRC\_TGT columns make up the primary key of the CDC\_STATS\_ALL table.

To enable querying the subscription metrics, a number of views referencing the CDC\_STATS\_ALL table are created:
- CDC\_STATS\_SRC\_DATASTORE - Source datastore metrics
- CDC\_STATS\_SRC\_DB\_WORKLOAD - Source database workload metrics
- CDC\_STATS\_SRC\_READ\_PARS - Source engine reading and parsing metrics
- CDC\_STATS\_SRC\_ENG - Source engine metrics
- CDC\_STATS\_SRC\_COMMS - Communication source metrics
- CDC\_STATS\_TGT\_COMMS - Communication target metrics
- CDC\_STATS\_TGT\_DATASTORE - Target datastore metrics
- CDC\_STATS\_TGT\_ENG - Target engine metrics
- CDC\_STATS\_TGT\_APP - Target apply metrics
- CDC\_STATS\_TGT\_APP\_THROUGPUT - Target apply throughput metrics (inserts, updates, deletes per second)

Specifically if you're interested in the throughput of the subscriptions over time, you may want to view the data in the CDC\_STATS\_TGT\_APP\_THROUGPUT view.

If you choose to log the metrics into flat files, the column layout of the flat files will be as follows:
dataStore|subscriptionName|collectTimestamp|metricSourceTarget|metricID|metricValue

## Subscription and datastore events
You can choose to log the subscription and datastore events into database table CDC\_EVENTS, or write them to a flat file. The data elements that are recorded are:
- Datastore name - Name of the source datastore of the subscription, or the name of the datastore for which the events are logged.
- Subscription name - Name of the subscription within the source datastore. Can be null if the entry represents a datastore event.
- Source or target indicator - Indicates whether the event applies to the source or the target
- Event type - Indicates the type of event (Information, Error, ...)
- Event timestamp - Date and time of the event
- Event message - Message that was sent to the event log

The database table CDC\_EVENTS has the following structure:
- SRC_DATASTORE
- SUBSCRIPTION
- SRC_TGT
- EVENT_ID
- EVENT_TYPE
- EVENT_TIMESTAMP
- EVENT_MESSAGE

This database table does not have a primary key.

If you choose to log the events into flat files, the column layout is as follows:
dataStore|subscriptionName||sourceTarget|eventID|eventType|eventMessage