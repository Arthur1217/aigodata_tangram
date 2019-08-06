package com.github.mengxianun.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.mengxianun.core.AbstractDataContext;
import com.github.mengxianun.core.Atom;
import com.github.mengxianun.core.Dialect;
import com.github.mengxianun.core.ResultStatus;
import com.github.mengxianun.core.data.DataSet;
import com.github.mengxianun.core.data.update.DefaultUpdateSummary;
import com.github.mengxianun.core.data.update.InsertSummary;
import com.github.mengxianun.core.data.update.UpdateSummary;
import com.github.mengxianun.core.dialect.DefaultDialect;
import com.github.mengxianun.core.request.Operation;
import com.github.mengxianun.core.resutset.DataResult;
import com.github.mengxianun.core.schema.ColumnType;
import com.github.mengxianun.core.schema.DefaultColumn;
import com.github.mengxianun.core.schema.DefaultSchema;
import com.github.mengxianun.core.schema.DefaultTable;
import com.github.mengxianun.core.schema.Schema;
import com.github.mengxianun.core.schema.TableType;
import com.github.mengxianun.jdbc.dialect.H2Dialect;
import com.github.mengxianun.jdbc.dialect.JdbcDialect;
import com.github.mengxianun.jdbc.dialect.MySQLDialect;
import com.github.mengxianun.jdbc.dialect.PostgreSQLDialect;
import com.github.mengxianun.jdbc.schema.JdbcColumnType;
import com.google.common.base.Strings;

public class JdbcDataContext extends AbstractDataContext {

	public static final String DATABASE_PRODUCT_POSTGRESQL = "PostgreSQL";
	public static final String DATABASE_PRODUCT_MYSQL = "MySQL";
	public static final String DATABASE_PRODUCT_HSQLDB = "HSQL Database Engine";
	public static final String DATABASE_PRODUCT_H2 = "H2";
	public static final String DATABASE_PRODUCT_SQLSERVER = "Microsoft SQL Server";
	public static final String DATABASE_PRODUCT_DB2 = "DB2";
	public static final String DATABASE_PRODUCT_DB2_PREFIX = "DB2/";
	public static final String DATABASE_PRODUCT_ORACLE = "Oracle";
	public static final String DATABASE_PRODUCT_HIVE = "Apache Hive";
	public static final String DATABASE_PRODUCT_SQLITE = "SQLite";
	public static final String DATABASE_PRODUCT_IMPALA = "Impala";

	public static final String INFORMATION_SCHEMA = "INFORMATION_SCHEMA";

	private static final Logger logger = LoggerFactory.getLogger(JdbcDataContext.class);
	// 当前线程操作的数据库连接
	private static final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();
	// 当前线程操作是否自动关闭连接
	private static final ThreadLocal<Boolean> closeConnection = new ThreadLocal<>();

	private final DataSource dataSource;
	private final TableType[] tableTypes;
	private final String catalog;
	private final String defaultSchema;

	private final String databaseProductName;
	private final String databaseProductVersion;

	private final String identifierQuoteString;
	private final boolean usesCatalogsAsSchemas;
	//
	private final QueryRunner runner;

	public JdbcDataContext(DataSource dataSource) {
		this(dataSource, TableType.DEFAULT_TABLE_TYPES);
	}

	public JdbcDataContext(DataSource dataSource, TableType[] tableTypes) {
		if (dataSource == null) {
			throw new IllegalArgumentException("DataSource cannot be null");
		}
		this.dataSource = dataSource;
		this.tableTypes = tableTypes;
		this.runner = new QueryRunner(dataSource);
		closeConnection.set(true);

		String catalogTemp = null;
		String defaultSchemaTemp = null;
		String databaseProductNameTemp = null;
		String databaseProductVersionTemp = null;
		boolean usesCatalogsAsSchemasTemp = false;
		String identifierQuoteStringTemp = null;

		// 如果是 DruidDataSource, 使用原生的连接获取数据库源信息. 因为DruidDataSource 不支持获取 Schema 信息.
		try (final Connection connection = dataSource instanceof DruidDataSource
				? DriverManager.getConnection(((DruidDataSource) dataSource).getUrl(), ((DruidDataSource) dataSource).getUsername(), ((DruidDataSource) dataSource).getPassword())
				: getConnection()) {
			catalogTemp = connection.getCatalog();
			defaultSchemaTemp = connection.getSchema();
			if (Strings.isNullOrEmpty(defaultSchemaTemp)) {
				usesCatalogsAsSchemasTemp = true;
				defaultSchemaTemp = catalogTemp;
			}

			DatabaseMetaData databaseMetaData = connection.getMetaData();

			databaseProductNameTemp = databaseMetaData.getDatabaseProductName();
			databaseProductVersionTemp = databaseMetaData.getDatabaseProductVersion();

			identifierQuoteStringTemp = databaseMetaData.getIdentifierQuoteString();
			if (identifierQuoteStringTemp != null) {
				identifierQuoteStringTemp = identifierQuoteStringTemp.trim();
			}
		} catch (SQLException e) {
			logger.debug("Unexpected exception during JdbcDataContext initialization", e);
		}

		catalog = catalogTemp;
		usesCatalogsAsSchemas = usesCatalogsAsSchemasTemp;
		defaultSchema = usesCatalogsAsSchemas ? catalog : defaultSchemaTemp;
		databaseProductName = databaseProductNameTemp;
		databaseProductVersion = databaseProductVersionTemp;
		identifierQuoteString = identifierQuoteStringTemp;

		dialect = createDialect(databaseProductName);

		initMetadata();
	}

	@Override
	public void initMetadata() {
		try (final Connection connection = getConnection()) {
			DatabaseMetaData databaseMetaData = connection.getMetaData();

			metadata.addSchema(new DefaultSchema(INFORMATION_SCHEMA, catalog));
			metadata.addSchema(new DefaultSchema(defaultSchema, catalog));

			String[] systemTypes = new String[] { TableType.SYSTEM_TABLE.name() };
			loadMetadata(databaseMetaData, catalog, INFORMATION_SCHEMA, "%", systemTypes, null);
			String[] types = Arrays.stream(tableTypes).map(TableType::name).toArray(String[]::new);
			loadMetadata(databaseMetaData, catalog, defaultSchema, "%", types, null);

		} catch (SQLException e) {
			throw new JdbcDataException(ResultStatus.DATASOURCE_EXCEPTION, e.getMessage());
		}
	}

	private void loadMetadata(DatabaseMetaData databaseMetaData, String catalog, String schemaPattern,
			String tableNamePattern, String[] types, String columnNamePattern) throws SQLException {
		// table metadata
		DefaultSchema schema = (DefaultSchema) metadata.getSchema(schemaPattern);
		ResultSet tablesResultSet = databaseMetaData.getTables(catalog, schemaPattern, "%", null);
		while (tablesResultSet.next()) {
			String tableName = tablesResultSet.getString(3);
			String tableTypeName = tablesResultSet.getString(4);
			TableType tableType = TableType.getTableType(tableTypeName);
			String remarks = tablesResultSet.getString(5);
			schema.addTable(new DefaultTable(tableName, tableType, schema, remarks));
		}

		// column metadata
		ResultSet columnsResultSet = databaseMetaData.getColumns(catalog, schemaPattern, "%", columnNamePattern);
		while (columnsResultSet.next()) {
			String columnTable = columnsResultSet.getString(3);
			String columnName = columnsResultSet.getString(4);
			String columnDataType = columnsResultSet.getString(5);
			String columnTypeName = columnsResultSet.getString(6);
			Integer columnSize = columnsResultSet.getInt(7);
			Boolean columnNullable = columnsResultSet.getBoolean(11);
			String columnRemarks = columnsResultSet.getString(12);

			DefaultTable table = (DefaultTable) metadata.getTable(schemaPattern, columnTable);
			ColumnType columnType = new JdbcColumnType(Integer.parseInt(columnDataType), columnTypeName);
			table.addColumn(
					new DefaultColumn(table, columnType, columnName, columnNullable, columnRemarks, columnSize));
		}
	}

	public Dialect createDialect(String databaseProductName) {
		Dialect dialectTemp = null;
		if (Strings.isNullOrEmpty(databaseProductName)) {
			dialectTemp = new DefaultDialect();
		} else {
			logger.debug("Database product name: {}", databaseProductName);

			switch (databaseProductName) {
			case DATABASE_PRODUCT_POSTGRESQL:
				dialectTemp = new PostgreSQLDialect(this);
				break;
			case DATABASE_PRODUCT_MYSQL:
				dialectTemp = new MySQLDialect(this);
				break;
			case DATABASE_PRODUCT_HSQLDB:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_H2:
				dialectTemp = new H2Dialect(this);
				break;
			case DATABASE_PRODUCT_SQLSERVER:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_DB2:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_DB2_PREFIX:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_ORACLE:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_HIVE:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_SQLITE:
				dialectTemp = new JdbcDialect(this);
				break;
			case DATABASE_PRODUCT_IMPALA:
				dialectTemp = new JdbcDialect(this);
				break;

			default:
				dialectTemp = new JdbcDialect(this);
				break;
			}
		}
		return dialectTemp;
	}

	public void startTransaction() throws SQLException {
		Connection conn = threadLocalConnection.get();
		if (conn == null) {
			conn = getConnection();
		}
		threadLocalConnection.set(conn);
		conn.setAutoCommit(false);
		closeConnection.set(false);
		logger.debug("Start new transaction.");
	}

	public Connection getConnection() throws SQLException {
		Connection conn = threadLocalConnection.get();
		if (conn == null) {
			return dataSource.getConnection();
		} else {
			return conn;
		}
	}

	public void commit() throws SQLException {
		Connection conn = threadLocalConnection.get();
		if (conn != null) {
			conn.commit();
			logger.debug("Transaction commit.");
		}
	}

	public void rollback() throws SQLException {
		Connection conn = threadLocalConnection.get();
		if (conn != null) {
			conn.rollback();
			logger.debug("Transaction rollback.");
		}
	}

	public void close() throws SQLException {
		Connection conn = threadLocalConnection.get();
		if (conn != null) {
			try {
				conn.close();
				logger.debug("Transaction close.");
			} finally {
				threadLocalConnection.remove();
				closeConnection.set(true);
			}
		}
	}

	/**
	 * 指定一组事务操作
	 * 
	 * @param atoms
	 * @throws SQLException
	 */
	@Override
	public void trans(Atom... atoms) {
		if (null == atoms) {
			return;
		}
		try {
			startTransaction();
			for (Atom atom : atoms) {
				atom.run();
			}
			commit();
		} catch (SQLException e) {
			try {
				rollback();
			} catch (SQLException e1) {
				throw new JdbcDataException("Transaction rollback failed.");
			}
		} finally {
			try {
				close();
			} catch (SQLException e) {
				logger.error("Transaction close failed.", e);
			}
		}
	}

	@Override
	public DataResult executeNative(Operation operation, String resource, String statement) {
		return executeSql(statement);
	}

	@Override
	protected DataSet query(String sql, Object... params) {
		try {
			List<Object[]> values = runner.query(sql, new ArrayListHandler(), params);
			return new JdbcDataSet(null, values);
		} catch (SQLException e) {
			Throwable realReasion = e;
			SQLException nextException = e.getNextException();
			if (nextException != null && nextException.getCause() != null) {
				realReasion = nextException.getCause();
			}
			logger.error(ResultStatus.DATASOURCE_SQL_FAILED.message(), realReasion);
			throw new JdbcDataException(ResultStatus.DATASOURCE_SQL_FAILED, realReasion.getMessage());
		}
	}

	@Override
	protected UpdateSummary insert(String sql, Object... params) {
		try {
			List<Map<String, Object>> insertContents = runner.insert(sql, new MapListHandler(), params);
			return new InsertSummary(insertContents);
		} catch (SQLException e) {
			Throwable realReasion = e;
			SQLException nextException = e.getNextException();
			if (nextException != null && nextException.getCause() != null) {
				realReasion = nextException.getCause();
			}
			logger.error(ResultStatus.DATASOURCE_SQL_FAILED.message(), realReasion);
			throw new JdbcDataException(ResultStatus.DATASOURCE_SQL_FAILED, realReasion.getMessage());
		}
	}

	@Override
	protected UpdateSummary update(String sql, Object... params) {
		try {
			int updateCount = runner.update(sql, params);
			return new DefaultUpdateSummary(updateCount);
		} catch (SQLException e) {
			Throwable realReasion = e;
			SQLException nextException = e.getNextException();
			if (nextException != null && nextException.getCause() != null) {
				realReasion = nextException.getCause();
			}
			logger.error(ResultStatus.DATASOURCE_SQL_FAILED.message(), realReasion);
			throw new JdbcDataException(ResultStatus.DATASOURCE_SQL_FAILED, realReasion.getMessage());
		}
	}

	@Override
	public Schema getDefaultSchema() {
		return getSchema(defaultSchema);
	}

	public TableType[] getTableTypes() {
		return tableTypes;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getDatabaseProductName() {
		return databaseProductName;
	}

	public String getDatabaseProductVersion() {
		return databaseProductVersion;
	}

	public String getIdentifierQuoteString() {
		return identifierQuoteString;
	}

	public QueryRunner getRunner() {
		return runner;
	}

}
