# Installation
The GitHub repository contains all components required to run the CollectCDCStats utility, including the Apache Commons and Log4j2 jar files. Besides the CDC Access Server or CDC Management Console, no further programs are needed. Classes have been compiled with Java 1.8, the version that comes with CDC Access Server 11.3.3.3. Therefore, after download, the utility can be used as is, and no compilation is required.

If you wish to use different versions of the included Apache projects, please refer to the [Compilation](#compilation) section. Because of a Java version dependency of Log4j2, the utility cannot be used with Access Server versions lower than 11.3.3.3.

## Downloading the utility
Download and un-zip the master zip file from GitHub through the following link: [Download Zip](https://github.com/fketelaars/IIDR-Stats-Warehouse/archive/master.zip).

## Required software versions
There is a strong dependency of the utility on CDC Access Server (or Management Console). At a minimum, the following versions are required:
- CDC Engines: 10.2.0 and above (CHCCLP must be supported)
- CDC Access Server (or Management Console): 11.3.3.3 (CHCCLP must be supported and JRE 1.8 included)

## Compilation
If you wish to build the toolkit yourself, the easiest method is to use Ant ([https://ant.apache.org/bindownload.cgi](https://ant.apache.org/bindownload.cgi)). 

Once you have this installed:
- Optionally, download new versions of the Apache libraries which are included under `opt/downloaded`
- Ensure that the ant executable is in the path
- Go to the directory where you unzipped the user exit master file
- Update the `CDC_AS_HOME` property in the `CollectCDCStats.properties` file to match the location where you installed the CDC Access Server
- Run `ant`
- First the sources will be compiled into their respective .class files and finally the class files are packaged into a jar file


