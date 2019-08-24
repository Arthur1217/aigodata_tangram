package com.github.mengxianun.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mengxianun.core.config.AssociationType;
import com.github.mengxianun.core.config.DataSourceConfig;
import com.github.mengxianun.core.config.GlobalConfig;
import com.github.mengxianun.core.config.TableConfig;
import com.github.mengxianun.core.exception.DataException;
import com.github.mengxianun.core.exception.JsonDataException;
import com.github.mengxianun.core.schema.Column;
import com.github.mengxianun.core.schema.Table;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public abstract class AbstractTranslator implements Translator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTranslator.class);
	protected final Map<String, DataContextFactory> factories = new HashMap<>();

	protected void init(String configFile) {
		init(convertToURL(configFile));
	}

	protected void init(URL configFileURL) {
		if (configFileURL != null) {
			readConfig(configFileURL);
		}
		try {
			parseAllTableConfig(App.Config.getString(GlobalConfig.TABLE_CONFIG_PATH));
		} catch (IOException e) {
			logger.error("Table config file parse error", e);
		}
	}

	protected URL convertToURL(String configFile) {
		try {
			URL configFileURL = Resources.getResource(configFile);
			App.Config.set(GlobalConfig.CONFIG_FILE, configFile);
			return configFileURL;
		} catch (Exception e) {
			logger.error(String.format("Config file [%s] parse error", configFile), e);
		}
		return null;
	}

	protected void readConfig(URL configFileURL) {
		try {
			String configurationFileContent = Resources.toString(configFileURL, StandardCharsets.UTF_8);
			JsonObject configurationJsonObject = new JsonParser().parse(configurationFileContent).getAsJsonObject();
			// 覆盖默认配置
			for (Entry<String, JsonElement> entry : configurationJsonObject.entrySet()) {
				App.Config.set(entry.getKey(), entry.getValue());
			}
			createDataContexts();
		} catch (IOException e) {
			logger.error(String.format("Config file [%s] parse error", configFileURL), e);
		}
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
				logger.info("Initialize data source [{}] successfully", dataSourceName);
			} else {
				logger.warn("Initialize data source [{}] failed, Could not find DataContextFactory with type [{}]",
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
			String sourceTableConfigDir = tableConfigPath + File.separator + dataContextName;
			parseSourceTableConfig(sourceTableConfigDir, dataContext);
		}
	}

	protected void parseSourceTableConfig(String sourceTableConfigDir, DataContext dataContext) throws IOException {
		URL tablesConfigURL = Thread.currentThread().getContextClassLoader().getResource(sourceTableConfigDir);
		if (tablesConfigURL == null) {
			return;
		}
		URI uri;
		try {
			uri = tablesConfigURL.toURI();
		} catch (URISyntaxException e) {
			throw new DataException(e);
		}
		if (uri.getScheme().equals("jar")) {
			try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
				Path sourceTableConfigDirPath = fileSystem.getPath("/WEB-INF/classes/" + sourceTableConfigDir);
				parseSourceTableConfig(sourceTableConfigDirPath, dataContext);
			}
		} else {
			Path sourceTableConfigDirPath = Paths.get(new File(uri).getPath());
			parseSourceTableConfig(sourceTableConfigDirPath, dataContext);
		}
	}

	protected void parseSourceTableConfig(Path sourceTableConfigDir, DataContext dataContext) throws IOException {
		try (Stream<Path> stream = Files.walk(sourceTableConfigDir, 1)) {
			stream.filter(Files::isRegularFile).forEach(path -> {
				parseTableConfigFile(path, dataContext);
			});
		}
	}

	/**
	 * 读取数据表配置文件, 文件名为表名
	 * 
	 * @param path
	 *            数据表配置文件路径
	 * @param dataContext
	 */
	protected void parseTableConfigFile(Path path, DataContext dataContext) {
		if (dataContext == null) {
			return;
		}
		String fileName = path.getFileName().toString();
		String tableName = fileName.substring(0, fileName.lastIndexOf("."));
		try {
			String content = Resources.toString(path.toUri().toURL(), StandardCharsets.UTF_8);
			JsonElement jsonElement = new JsonParser().parse(content);
			JsonObject tableConfig = jsonElement.getAsJsonObject();
			Table table = dataContext.getTable(tableName);
			if (table == null) {
				logger.warn("Table [{}] from [{}] does not exist", tableName, fileName);
				return;
			}
			table.setConfig(tableConfig);
			if (tableConfig.has(TableConfig.COLUMNS)) {
				JsonObject columnsConfig = tableConfig.get(TableConfig.COLUMNS).getAsJsonObject();
				for (String columnName : columnsConfig.keySet()) {
					Column column = dataContext.getColumn(tableName, columnName);
					if (column != null) {
						JsonObject columnConfig = columnsConfig.get(columnName).getAsJsonObject();
						column.setConfig(columnConfig);
						// 添加 Relationship
						if (columnConfig.has(TableConfig.COLUMN_ASSOCIATION)) {
							JsonObject associationConfig = columnConfig
									.getAsJsonObject(TableConfig.COLUMN_ASSOCIATION);
							String targetTableName = associationConfig
									.getAsJsonPrimitive(TableConfig.ASSOCIATION_TARGET_TABLE).getAsString();
							String targetColumnName = associationConfig
									.getAsJsonPrimitive(TableConfig.ASSOCIATION_TARGET_COLUMN).getAsString();
							AssociationType associationType = associationConfig
									.has(TableConfig.ASSOCIATION_TYPE)
											? AssociationType.from(associationConfig
													.getAsJsonPrimitive(TableConfig.ASSOCIATION_TYPE)
													.getAsString())
											: AssociationType.ONE_TO_ONE;
							Column targetColumn = dataContext.getColumn(targetTableName, targetColumnName);
							// 添加主外表的关联
							dataContext.addRelationship(column, targetColumn, associationType);
						}
					}
				}
			}
		} catch (JsonIOException | JsonSyntaxException | IOException e) {
			throw new DataException(String.format("Parsing table config file [%s] failed", path), e);
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
		logger.debug("Request: {}", json);

		// 设置当前线程的上下文
		com.github.mengxianun.core.JsonParser parser = new com.github.mengxianun.core.JsonParser(json);
		String sourceName = parser.parseSource();
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

		// Run
		Action action = parser.parse();
		action.build();

		DataResultSet dataResultSet = execute(dataContext, action);

		// Done
		Duration duration = stopwatch.stop().elapsed();

		logger.debug("Operation is completed, taking {} milliseconds", duration.toMillis());

		// 清理当前线程的上下文
		App.cleanup();

		return dataResultSet;
	}

	protected abstract DataResultSet execute(DataContext dataContext, Action action);

	/**
	 * 释放资源
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
