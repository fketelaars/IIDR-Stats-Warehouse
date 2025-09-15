# Usage
Once the tool has been configured, you can perform collect the statistics using the shell/command script that is provided in the utility's home directory.

* Linux/Unix: `CollectCDCStats.sh [-cfg <Datastores properties file>][-p <properties file>] [-d]`
* Windows: `CollectCDCStats.cmd  [-cfg <Datastores properties file>][-p <properties file>] [-d]`


## Parameters
- p: Optional. Specifies the name of the properties file from which the configuration will be read. This properties file must exist in the `conf` directory. By default, the `CollectCDCStats.properties` file is used.
- d: Optional. Displays debug messages for troubleshooting.
- cfg: Optional. Specifies the name of the properties file from which the datastores and subscriptions will be read. This properties file must exist in the `conf` directory. By default, the `DatastoresAndSubscriptions.properties` file is used.

## Command example
`CollectCDCStats.sh`

The command connects to the Access Server and then the CDC_DB2 datastore. Subsequently, it retrieves the statistics for subscriptions S1, S2 and S3 and then writes them to the database specified in the properties file.

## scheduled script

For scheduled execution, use `CollectStats_Loop.sh`, which automatically re-triggers the process at the desired frequency. The frequency interval can be configured by adjusting the delay parameter in the loop script.
