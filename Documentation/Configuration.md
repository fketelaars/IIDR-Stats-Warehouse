# Configuration
In most scenarios you will need to perform a few configuration tasks:
- Update the configuration properties, reviewing the Access Server or Management Console path
- Set the credentials for the CDC Access Server
- Choose the loggers you want to use for the subscription status, metrics and events
- Set the properties for each of the loggers (if applicable)

## Setting the configuration properties
Update the `conf/CollectCDCStats.properties` file with your favourite editor and set the properties to reflect your environment.

### Access Server properties
* CDC\_AS\_HOME: Home (main) directory of the Access Server or Management Console. This directory must have a `lib` subdirectory that holds the CHCCLP jar files and a directory with the Java Runtime Engine that will be used for the utility. If your directory path contains blanks or other special characters, please enclose the path in double quotes ("). If you run the utility on Windows, you would typically have to enclose the path in double quotes. Example: `CDC_AS_HOME="C:\Program Files (x86)\IBM\IS CDC MC 1133"`
* asHostName: Host name or IP address of the server running the Access Server.
* asUserName: User name to connect to the Access Server.
* asPassword: Password for the Access Server user. Please specify the password in its readable format; when the utility runs it will automatically encrypt the password and update the property.

### Configure the loggers
For each of the monitored items: subscription status, metrics and event log, you can specify the logger you wish to use. The tool includes the following loggers:
* com.ibm.replication.iidr.warehouse.logging.LogCsv: this logger writes all entries to CSV files
* com.ibm.replication.iidr.warehouse.logging.LogFlatFile: this logger writes entries to flat files in the format specified in the log4j2.xml configuration file
* com.ibm.replication.iidr.warehouse.logging.LogDatabase: this logger writes entries to database tables

## CSV logger (com.ibm.replication.iidr.warehouse.logging.LogCsv) configuration
For logging into CSV files, one property is configured in the `conf/CollectCDCStats.properties` file:
* com.ibm.replication.iidr.warehouse.logging.LogCsv.csvSeparator: the separation character to be used between the logged columns. The default separation character is `|`

Furthermore, the CSV logging utilizes the Apache log4j2 logger. Please refer to the `conf/log4j2.xml` configuration file which determines in which directory and file names the CSV records will be written. By default, the log records are written into 3 different types of files: Statistics (for the metrics), SubStatus (for the subscription status) and Events (for the datastore and subscription events. Statistics, status and events are collected in log files which are zipped and archived every 1 hour and automatically removed after 7 days. By changing the properties in the `conf/log4j2.xml` file, you can fully control in which directory files are kept and even keep events and metrics from different datastores in different directory structures. Please refer to the ([http://logging.apache.org/log4j/2.x/manual/appenders.html](http://logging.apache.org/log4j/2.x/manual/appenders.html)) for more information.

## Flat file logger (com.ibm.replication.iidr.warehouse.logging.LogFlatFile) configuration
The flat file logger is fully configured via the `conf/log4j2.xml` configuration file and writes entries in the following format:
`timestamp,key=value1,key2=value2,key3=value3,...`

Please refer to the `conf/log4j2.xml` configuration file which determines in which directory and file names the flat files records will be written. By default, the log records are written into 3 different types of files: Statistics (for the metrics), SubStatus (for the subscription status) and Events (for the datastore and subscription events). All log files have the extension `.log`. The log files are zipped and archived every 1 hour and automatically removed after 7 days. By changing the properties in the `conf/log4j2.xml` file, you can fully control in which directory files are kept and even keep events and metrics from different datastores in different directory structures. Please refer to the ([http://logging.apache.org/log4j/2.x/manual/appenders.html](http://logging.apache.org/log4j/2.x/manual/appenders.html)) for more information.

### Database logger (com.ibm.replication.iidr.warehouse.logging.LogDatabase) configuration
If you wish to store subscription status, metrics or event logs in database tables, you need to set the properties to connect to the database. Also, the database driver's JAR file must be made available to the tool by copying it into the `opt/downloaded` directory.

The database properties are:
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbHostName: Host name or IP address of the server running the database that will hold the statistics
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbPort: Port of the database
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbDatabase: Name of the database
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbUserName: User name to connect to the database hold the statistics
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbPassword: Password for the database user. Please specify the password in its readable format; when the utility runs it will automatically encrypt the password and update the property.
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbDriverName: Full name of the Java class that defines the database driver. For DB2 this is `com.ibm.db2.jcc.DB2Driver`
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbUrl: JDBC connection string to use. This string can contain properties from this property file by enclosing them in curly brackets and prefixing them with a $ sign. Example for DB2: `jdbc:db2://${dbHostName}:${dbPort}/${dbDatabase}`
* com.ibm.replication.iidr.warehouse.logging.LogDatabase.dbSchema: Schema that holds the statistics and status tables

