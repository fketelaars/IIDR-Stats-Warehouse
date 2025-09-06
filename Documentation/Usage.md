# Usage
Once the tool has been configured, you can perform collect the statistics using the shell/command script that is provided in the utility's home directory.

* Linux/Unix: `CollectCDCStats.sh -ds <source datastore(s)> [-p <properties file>] [-d]`
* Windows: `CollectCDCStats.cmd -ds <source datastore(s)> [-p <properties file>] [-d]`


## Parameters
- ds: Specifies the source datastores of the subscriptons you wish to export. Multiple datastores can be specified, separated by commas.
- p: Optional. Specifies the name of the properties file from which the configuration will be read. This properties file must exist in the `conf` directory. By default, the `CollectCDCStats.properties` file is used.
- d: Optional. Displays debug messages for troubleshooting.

## Command example
`CollectCDCStats.sh -ds CDC_DB2,CDC_DB3,CDC_DB4`

The command connects to the Access Server and then the CDC_DB2 datastore. Subsequently, it retrieves the statistics for subscriptions S1, S2 and S3 and then writes them to the database specified in the properties file.
