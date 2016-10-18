# Usage
Once the tool has been configured, you can perform collect the statistics using the shell/command script that is provided in the utility's home directory.

* Linux/Unix: `CollectCDCStats.sh -ds <source datastore> [-s subscription(s)] [-p <properties file>] [-d]`
* Windows: `CollectCDCStats.cmd -ds <source datastore> [-s subscription(s)] [-p <properties file>] [-d]`


## Parameters
- ds: Specifies the source datastore of the subscriptons you wish to export.
- s: Optional. Specifies which subscriptions you wish to monitor. Multiple subscriptions can be specified, separated by commas. If you do not specify this parameter, all subscriptions sourcing the specified datastore will be monitored.
- p: Optional. Specifies the name of the properties file from which the configuration will be read. This properties file must exist in the `conf` directory. By default, the `CollectCDCStats.properties` file is used.
- d: Optional. Displays debug messages for troubleshooting.

## Command example
`CollectCDCStats.sh -ds CDC_DB2 -s S1,S2,S3`

The command connects to the Access Server and then the CDC_DB2 datastore. Subsequently, it retrieves the statistics for subscriptions S1, S2 and S3 and then writes them to the database specified in the properties file.