# IBM InfoSphere Data Replication CDC - Statistics Warehouse
==============

In a world where businesses increasingly require timely access to current data before making business critical decisions, InfoSphere Data Replication facilitates the capturing and delivery of change data from the source systems on which it is located to the target systems and applications where it is required.

This toolkit captures the InfoSphere Data Replication CDC (CDC) status, metrics and events and stores them into fact tables in a database of your choice, or logs them into flat files. 

When choosing database tables for storing the assets, a few fact tables and a number of views have been pre-defined for each of the information categories. You can use the pre-defined views for interrogating the most-common metrics, but you can create additional views as you see fit.

If you're using external monitoring tools, such as products from the IBM Tivoli stack or Splunk, you may choose to log status, metrics and events into flat files and have the monitoring solution process them for central consolidation and reporting. 

## Installation of the utility
Installation instructions can be found here: [Installation](Documentation/Installation.md)

## Configuration
Once installation is done, here you can find how to use configure the utility to write the statistics into a database or log files: [Configuring the utility](Documentation/Configuration.md)

## Using the utility
Here you can find examples on how to start collecting the statistics: [Usage](Documentation/Usage.md)

## Reference
In this document you will find an overview of the collected information and how to access it for monitoring: [Reference](Documentation/Reference.md)