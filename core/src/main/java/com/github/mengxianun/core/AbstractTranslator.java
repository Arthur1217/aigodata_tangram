package com.github.mengxianun.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mengxianun.core.Configuration.Builder;
import com.github.mengxianun.core.config.ColumnConfig;
import com.github.mengxianun.core.config.DataSourceConfig;
import com.github.mengxianun.core.config.GlobalConfig;
import com.github.mengxianun.core.data.Summary;
import com.github.mengxianun.core.exception.DataException;
import com.github.mengxianun.core.exception.JsonDataException;
import com.github.mengxianun.core.exception.PermissionException;
import com.github.mengxianun.core.parser.ActionParser;
import com.github.mengxianun.core.parser.ParserFactory;
import com.github.mengxianun.core.parser.SimpleParser;
import com.github.mengxianun.core.parser.info.SimpleInfo;
import com.github.mengxianun.core.permission.PermissionCheckResult;
import com.github.mengxianun.core.permission.PermissionChecker;
import com.github.mengxianun.core.permission.PermissionPolicy;
import com.github.mengxianun.core.resutset.DefaultDataResultSet;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class AbstractTranslator implements Translator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTranslator.class);
	protected final Map<String, DataContextFactory> factories = new HashMap<>();

	protected void init(String configFile) {
		readConfig(configFile);
		readTableConfig(App.Config.getString(GlobalConfig.TABLE_CONFIG_PATH));
	}

	private void readConfig(String configFile) {
		logger.info("Read config file [{}]", configFile);
		String configFileContent = null;
		try {
			// Read config file from classpath
			configFileContent = readConfigFromClasspath(configFile);
		} catch (Exception e) {
			logger.warn("Config file [{}] not found in classpath", configFile);
			try {
				// Read config file from filesystem
				configFileContent = readConfigFromFileSystem(configFile);
			} catch (IOException e1) {
				logger.warn("Config file [{}] not found in filesystem", configFile);
			}
		}

		if (Strings.isNullOrEmpty(configFileContent)) {
			throw new DataException("Config file [%s] read failed", configFile);
		}

		App.Config.set(GlobalConfig.CONFIG_FILE, configFile);

		JsonObject configurationJsonObject = new Gson().fromJson(configFileContent, JsonObject.class);
		for (Entry<String, JsonElement> entry : configurationJsonObject.entrySet()) {
			App.Config.set(entry.getKey(), entry.getValue());
		}
		///////////////
		// optimize
		///////////////
		Configuration configuration = parseConfiguration(configFile, configurationJsonObject);
		App.setConfiguration(configuration);
		createDataContexts();

		if (configuration.metadataRefreshPolicy() == RefreshPolicy.MINUTES) {
			int metadataRefreshInterval = configuration.metadataRefreshInterval();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(() -> App.getDataContexts().values().forEach(DataContext::refresh),
					metadataRefreshInterval, metadataRefreshInterval, TimeUnit.MINUTES);
		}
	}

	private String readConfigFromClasspath(String configFile) throws IOException {
		return Resources.toString(Resources.getResource(configFile), StandardCharsets.UTF_8);
	}

	private String readConfigFromFileSystem(String configFile) throws IOException {
		Path path = Paths.get(configFile);
		if (!path.isAbsolute()) {
			String parentDir = System.getProperty("user.dir");
			String realPath = parentDir + File.separator + configFile;
			path = Paths.get(realPath);
		}
		return new String(Files.readAllBytes(path));
	}

	private void readTableConfig(String tableConfigPath) {
		try {
			parseAllTableConfig(tableConfigPath);
		} catch (IOException e) {
			logger.error("Table config file read error", e);
		}
	}

	protected Configuration parseConfiguration(String configFile, JsonObject configurationJsonObject) {
		Builder builder = Configuration.builder();
		builder.configFile(configFile);
		// datasources
		if (configurationJsonObject.has(GlobalConfig.DATASOURCES)) {
			builder.datasources(configurationJsonObject.getAsJsonObject(GlobalConfig.DATASOURCES).toString());
		} else {
			throw new DataException("Data sources are required");
		}
		if (configurationJsonObject.has(GlobalConfig.DEFAULT_DATASOURCE)) {
			builder.defaultDatasource(configurationJsonObject.get(GlobalConfig.DEFAULT_DATASOURCE).getAsString());
		}
		if (configurationJsonObject.has(GlobalConfig.SQL)) {
			builder.sqlEnabled(configurationJsonObject.get(GlobalConfig.SQL).getAsBoolean());
		}
		if (configurationJsonObject.has(GlobalConfig.NATIVE)) {
			builder.nativeEnabled(configurationJsonObject.get(GlobalConfig.NATIVE).getAsBoolean());
		}
		if (configurationJsonObject.has(GlobalConfig.TABLE_CONFIG_PATH)) {
			builder.tableConfigPath(configurationJsonObject.get(GlobalConfig.TABLE_CONFIG_PATH).getAsString());
		}
		if (configurationJsonObject.has(GlobalConfig.TABLE_ALIAS_EXPRESSION)) {
			builder.tableAliasExpression(
					configurationJsonObject.get(GlobalConfig.TABLE_ALIAS_EXPRESSION).getAsString());
		}
		if (configurationJsonObject.has(GlobalConfig.ASSOCIATION_CONNECTOR)) {
			builder.associationConnector(configurationJsonObject.get(GlobalConfig.ASSOCIATION_CONNECTOR).getAsString());
		}
		if (configurationJsonObject.has(GlobalConfig.PERMISSION_POLICY)) {
			String permissionPolicy = configurationJsonObject.get(GlobalConfig.PERMISSION_POLICY).getAsString();
			builder.permissionPolicy(PermissionPolicy.from(permissionPolicy));
		}
		if (configurationJsonObject.has(GlobalConfig.COLUMNS)) {
			Map<String, ColumnConfigInfo> columnConfigInfos = new HashMap<>();
			JsonArray columns = configurationJsonObject.getAsJsonArray(GlobalConfig.COLUMNS);
			for (JsonElement columnElement : columns) {
				JsonObject columnObject = columnElement.getAsJsonObject();
				if (!columnObject.has(ColumnConfig.NAME)) {
					continue;
				}
				ColumnConfigInfo.Builder columnConfigInfoBuilder = ColumnConfigInfo.builder();
				String columnName = columnObject.get(ColumnConfig.NAME).getAsString();
				columnConfigInfoBuilder.name(columnName);
				if (columnObject.has(ColumnConfig.TIME_FORMAT)) {
					String timeFormat = columnObject.get(ColumnConfig.TIME_FORMAT).getAsString();
					columnConfigInfoBuilder.timeFormat(timeFormat);
				}
				columnConfigInfos.put(columnName, columnConfigInfoBuilder.build());
			}
			builder.columnConfigInfos(columnConfigInfos);
		}
		if (configurationJsonObject.has(GlobalConfig.METADATA_REFRESH_POLICY)) {
			RefreshPolicy metadataRefreshPolicy = RefreshPolicy
					.from(configurationJsonObject.get(GlobalConfig.METADATA_REFRESH_POLICY).getAsString());
			builder.metadataRefreshPolicy(metadataRefreshPolicy);
			if (configurationJsonObject.has(GlobalConfig.METADATA_REFRESH_INTERVAL)) {
				int metadataRefreshInterval = configurationJsonObject.get(GlobalConfig.METADATA_REFRESH_INTERVAL)
						.getAsInt();
				builder.metadataRefreshInterval(metadataRefreshInterval);
			}
		}
		if (configurationJsonObject.has(GlobalConfig.JOIN_ON_MULTI_COLUMN)) {
			builder.joinOnMultiColumn(configurationJsonObject.get(GlobalConfig.JOIN_ON_MULTI_COLUMN).getAsBoolean());
		}
		return builder.build();
	}

	protected void createDataContexts() {
		discoverFromClasspath();

		JsonObject dataSourcesJsonObject = App.Config.getJsonObject(GlobalConfig.DATASOURCES);
		for (Entry<String, JsonElement> entry : dataSourcesJsonObject.entrySet()) {
			String dataSourceName = entry.getKey();
			JsonObject dataSourceJsonObject = dataSourcesJsonObject.getAsJsonObject(dataSourceName);
			
			if (App.hasDataContext(dataSourceName)) {
				logger.info("Data source [{}] already exists", dataSourceName);
				continue;
			}
			String type = parseDataContextType(dataSourceJsonObject);
			if (type == null || !factories.containsKey(type)) {
				String message = String.format("Data source [%s] type [%s] is not supported", dataSourceName, type);
				logger.error(message, new DataException());
				continue;
			}
			DataContextFactory dataContextFactory = factories.get(type);
			if (dataContextFactory != null) {
				dataSourceJsonObject.remove(DataSourceConfig.TYPE);
				DataContext dataContext = dataContextFactory.create(dataSourceJsonObject);
				addDataContext(dataSourceName, dataContext);
				logger.info("Create data source [{}] successfully", dataSourceName);
			} else {
				logger.warn("Create data source [{}] failed, Could not find DataContextFactory with type [{}]",
						dataSourceName, type);
			}
		}

		App.initDefaultDataSource();
	}

	/**
	 * 解析数据源类型, 如果指定了 type 属性, 以 指定的 type 为准. 如果没有指定 type 属性, 则从url属性中解析数据源类型
	 * 
	 * @param dataSourceJsonObject
	 * @return DataContext Type
	 */
	protected String parseDataContextType(JsonObject dataSourceJsonObject) {
		String type = null;
		if (dataSourceJsonObject.has(DataSourceConfig.TYPE)) {
			type = dataSourceJsonObject.get(DataSourceConfig.TYPE).getAsString();
		} else {
			if (dataSourceJsonObject.has(DataSourceConfig.URL)) {
				String url = dataSourceJsonObject.get(DataSourceConfig.URL).getAsString();
				for (String factoryType : factories.keySet()) {
					if (url.contains(factoryType)) {
						type = factoryType;
						break;
					}
				}
			}
		}
		return type;
	}

	protected void parseAllTableConfig(String tableConfigPath) throws IOException {
		Map<String, DataContext> dataContexts = App.getDataContexts();
		for (Map.Entry<String, DataContext> entry : dataContexts.entrySet()) {
			String dataContextName = entry.getKey();
			DataContext dataContext = entry.getValue();
			String sourceTableConfigDir = tableConfigPath + '/' + dataContextName;
			ConfigHelper.parseSourceTableConfig(sourceTableConfigDir, dataContext);
		}
	}

	protected void addDataContext(String name, DataContext dataContext) {
		if (App.hasDataContext(name)) {
			throw new DataException(String.format("DataContext [%s] already exists", name));
		}
		App.addDataContext(name, dataContext);
		if (!App.Config.has(GlobalConfig.DEFAULT_DATASOURCE)
				|| Strings.isNullOrEmpty(App.Config.getString(GlobalConfig.DEFAULT_DATASOURCE))) {
			String defaultDataSourceName = App.getDataContexts().keySet().iterator().next();
			App.Config.set(GlobalConfig.DEFAULT_DATASOURCE, defaultDataSourceName);
		}
	}

	protected void discoverFromClasspath() {
		final ServiceLoader<DataContextFactory> serviceLoader = ServiceLoader.load(DataContextFactory.class);
		for (DataContextFactory factory : serviceLoader) {
			addFactory(factory);
		}
	}

	public void addFactory(DataContextFactory factory) {
		factories.put(factory.getType(), factory);
	}

	public void reInit() {
		init(App.Config.getString(GlobalConfig.CONFIG_FILE));
	}

	@Override
	public DataResultSet translate(String json) {
		logger.debug("Request: \n{}", json);

		// Sets the context of the current thread
		SimpleInfo simpleInfo = SimpleParser.parse(json);
		String sourceName = null;
		if (simpleInfo.source() != null) {
			sourceName = simpleInfo.source().source();
		} else if (simpleInfo.table() != null) {
			sourceName = simpleInfo.table().source();
		}
		if (Strings.isNullOrEmpty(sourceName)) {
			sourceName = App.getDefaultDataSource();
		}
		if (!App.hasDataContext(sourceName)) {
			throw new JsonDataException(ResultStatus.DATASOURCE_NOT_EXIST.fill(sourceName));
		}
		DataContext dataContext = App.getDataContext(sourceName);
		App.setCurrentDataContext(dataContext);

		// Stopwatch
		Stopwatch stopwatch = Stopwatch.createStarted();

		// Permission valid
		simpleInfo = checkPermission(simpleInfo);

		ActionParser actionParser = ParserFactory.getActionParser(simpleInfo, dataContext);
		NewAction newAction = actionParser.parse();

		Summary summary = newAction.execute();
		DataResultSet dataResultSet = new DefaultDataResultSet(summary);

		// Done
		Duration duration = stopwatch.stop().elapsed();

		logger.debug("Operation completed in {} milliseconds", duration.toMillis());

		// Cleans up the context of the current thread
		App.cleanup();

		return dataResultSet;
	}

	private SimpleInfo checkPermission(SimpleInfo simpleInfo) {
		PermissionCheckResult checkWithResult = PermissionChecker.checkWithResult(simpleInfo);
		if (!checkWithResult.pass()) {
			throw new PermissionException();
		}
		return checkWithResult.simpleInfo();
	}

	protected abstract DataResultSet execute(DataContext dataContext, Action action);

	/**
	 * Release resources
	 */
	@PreDestroy
	protected void destroy() {
		logger.info("Destroy all DataContext...");
		for (Map.Entry<String, DataContext> entry : App.getDataContexts().entrySet()) {
			String name = entry.getKey();
			DataContext dataContext = entry.getValue();
			dataContext.destroy();
			logger.info("DataContext [{}] destroyed", name);

		}
		logger.info("All DataContext is already destroyed");
	}

}
