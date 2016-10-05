package com.ibm.replication.iidr.warehouse.logging;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import com.ibm.replication.iidr.utils.Settings;

public class LogDatabase extends LogInterface {

	// Database connection parameters
	private Connection con = null;
	private PreparedStatement insertSubStatus;
	private PreparedStatement insertSubMetrics;

	public LogDatabase(Settings settings)
			throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
		super(settings);
		connectDatabase();
	}

	/**
	 * Establishes a connection to the database.
	 * 
	 * @throws SQLException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	private void connectDatabase()
			throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		try {
			Properties props = new Properties();
			props.put("user", settings.dbUserName);
			props.put("password", settings.dbPassword);
			logger.debug("Database URL: " + settings.dbUrl);
			Class.forName(settings.dbDriverName).newInstance();
			con = DriverManager.getConnection(settings.dbUrl, props);

			logger.debug(settings.dbDriverName + " Loading");
			logger.debug("Connecting to url : " + settings.dbUrl + " using user name " + settings.dbUserName);
			DatabaseMetaData dbmd = con.getMetaData();
			logger.debug("DatabaseProductName: " + dbmd.getDatabaseProductName());
			logger.debug("DatabaseProductVersion: " + dbmd.getDatabaseProductVersion());
			// Disable auto-commit, this should be done in the harden() method
			logger.debug("Disabling auto-commit");
			con.setAutoCommit(false);
		} catch (SQLException esql) {
			logger.error(esql.toString());
			disconnectDatabase();
		} catch (ClassNotFoundException esql) {
			logger.error(esql.toString());
			disconnectDatabase();
		} catch (Exception e) {
			logger.error("Connecting to database failed, error: " + e.toString());
		}
	}

	private void disconnectDatabase() {
		if (con != null) {
			try {
				con.rollback();
			} catch (SQLException ignore) {
				logger.debug("Error during rollback: " + ignore.getMessage());
			}
			try {
				con.close();
			} catch (SQLException ignore) {
				logger.debug("Error while closing connection to database: " + ignore.getMessage());
			}
		}
	}

	/**
	 * Logs the status of the subscription into a database table
	 * 
	 * @throws SQLException
	 */
	@Override
	public void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) throws SQLException {
		// Only try to insert the status if the connection has been established
		if (con != null) {
			// Prepare statement if this is the first time
			if (insertSubStatus == null)
				insertSubStatus = con.prepareStatement("insert into " + settings.dbSchema + ".CDC_SUB_STATUS "
						+ "(SRC_DATASTORE,SUBSCRIPTION,COLLECT_TS,SUBSCRIPTION_STATUS) " + "VALUES (?,?,?,?)");

			// Write the subscription status into the table
			insertSubStatus.setString(1, dataStore);
			insertSubStatus.setString(2, subscriptionName);
			insertSubStatus.setTimestamp(3, collectTimestamp);
			insertSubStatus.setString(4, subscriptionState);
			insertSubStatus.execute();
		}
	}

	/**
	 * Logs the metrics into the database
	 */
	@Override
	public void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) throws SQLException {
		// Only try to insert the status if the connection has been established
		if (con != null) {
			if (insertSubMetrics == null)
				insertSubMetrics = con.prepareStatement("insert into " + settings.dbSchema + ".CDC_STATS_ALL "
						+ "(SRC_DATASTORE,SUBSCRIPTION,COLLECT_TS,SRC_TGT,METRIC_ID,METRIC_VALUE) "
						+ "VALUES (?,?,?,?,?,?)");

			// Write the metrics into the table
			insertSubMetrics.setString(1, dataStore);
			insertSubMetrics.setString(2, subscriptionName);
			insertSubMetrics.setTimestamp(3, collectTimestamp);
			insertSubMetrics.setString(4, metricSourceTarget);
			insertSubMetrics.setInt(5, metricID);
			insertSubMetrics.setLong(6, metricValue);
			insertSubMetrics.execute();
		}
	}

	/**
	 * Hardens (commits) the transaction into the database
	 */
	@Override
	public void harden() throws SQLException {
		if (con != null)
			con.commit();
	}

	/**
	 * Final processing (disconnect from the database)
	 */
	@Override
	public void finish() {
		logger.debug("Finalizing processing for logging to database");
		disconnectDatabase();
	}

}
