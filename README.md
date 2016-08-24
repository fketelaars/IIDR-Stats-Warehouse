# IBM InfoSphere Data Replication CDC - Statistics Warehouse

In a world where businesses increasingly require timely access to current data before making business critical decisions, InfoSphere Data Replication facilitates the capturing and delivery of change data from the source systems on which it is located to the target systems and applications where it is required.

This toolkit captures the InfoSphere Data Replication CDC (CDC) metrics and status and stores them into fact tables in the database of choice. A number of views have been pre-defined for the most-common metrics one would want to monitor, but additional views can be created as well.

## Installation
The GitHub repository contains all components required to run the CollectCDCStats utility, including the Apache Commons and Log4j jar files. Besides the CDC Access Server or CDC Management Console, no further programs are needed. Classes have been compiled with Java 1.8, the version that comes with CDC Access Server 11.3.3. 

If you wish to use different versions of the included Apache projects, or use it with an older version of the CDC Access Server, please refer to the [Compilation](#compilation) section.

Download and unzip the master zip file from GitHub through the following link: [Download Zip](https://github.com/fketelaars/IIDR-Stats-Warehouse/archive/master.zip).

### Required software versions
There is a strong dependency of the utility on CDC Access Server (or Management Console). At a minimum, the following versions are required:
- CDC Engines: 10.2.0 and above (CHCCLP must be supported)
- CDC Access Server (or Management Console): 10.2.0 and higher (CHCCLP must be supported)

## Configuration
In most scenarios you will need to perform two configuration tasks:
- Update the configuration properties, reviewing the Access Server or Management Console path
- Update the credentials for the Access Server
- Specify the database that will hold the statistics, along with the user and password to connect
- Copy the database's JDBC driver into the utility's lib directory so that it is included in the classpath
- Create the statistics and status fact tables and their views in the database. Sample scripts for various databases have been included in the conf directory.

### Setting the configuration properties
Update the `conf/CollectCDCStats.properties` file with your favourite editor and set the properties to reflect your environment. At a minimum, set the following properties:
* CDC\_AS\_HOME: Home (main) directory of the Access Server or Management Console. This directory must have a `lib` subdirectory that holds the CHCCLP jar files and a directory with the Java Runtime Engine that will be used for the utility. If your directory path contains blanks or other special characters, please enclose the path in double quotes (").
* asHostName: Host name or IP address of the server running the Access Server.
* asUserName: User name to connect to the Access Server.
* asPassword: Password for the Access Server user. Please specify the password in its readable format; when the utility runs it will automatically encrypt the password and update the property.
* dbHostName: Host name or IP address of the server running the database that will hold the statistics
* dbPort: Port of the database
* dbDatabase: Name of the database
* dbUserName: User name to connect to the database hold the statistics
* dbPassword: Password for the database user. Please specify the password in its readable format; when the utility runs it will automatically encrypt the password and update the property.
* dbDriverName: Full name of the Java class that defines the database driver. For DB2 this is `com.ibm.db2.jcc.DB2Driver`
* dbUrl: JDBC connection string to use. This string can contain properties from this property file by enclosing them in curly brackets and prefixing them with a $ sign. Example for DB2: `jdbc:db2://${dbHostName}:${dbPort}/${dbDatabase}`
* dbSchema: Schema that holds the statistics and status tables


## Usage
Once the tool has been configured, you can perform collect the statistics using the shell/command script that is provided in the utility's home directory.

* Linux/Unix: `CollectCDCStats.sh -ds <source datastore> [-s subscription(s)] [-d]`


### Parameters
- ds: Specifies the source datastore of the subscriptons you wish to export.
- s: Optional. Specifies which subscriptions you wish to monitor. Multiple subscriptions can be specified, separated by commas. If you do not specify this parameter, all subscriptions sourcing the specified datastore will be monitored.
- d: Optional. Displays debug messages for troubleshooting.

### Command example
`CollectCDCStats.sh -ds CDC_DB2 -s S1,S2,S3`

The command connects to the Access Server and then the CDC_DB2 datastore. Subsequently, it retrieves the statistics for subscriptions S1, S2 and S3 and then writes them to the database specified in the properties file.


## Compilation
If you wish to build the toolkit yourself, the easiest method is to use Ant ([https://ant.apache.org/bindownload.cgi](https://ant.apache.org/bindownload.cgi)). 


Once you have this installed:
- Optionally, download new versions of the Apache libraries which are included under `opt/downloaded`
- Ensure that the ant executable is in the path
- Go to the directory where you unzipped the user exit master file
- Update the `CDC_AS_HOME` property in the `CollectCDCStats.properties` file to match the location where you installed the CDC Access Server
- Run `ant`
- First the sources will be compiled into their respective .class files and finally the class files are packaged into a jar 


