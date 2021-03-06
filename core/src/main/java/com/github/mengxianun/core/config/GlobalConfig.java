package com.github.mengxianun.core.config;

public final class GlobalConfig {

	private GlobalConfig() {
		throw new AssertionError();
	}

	// 配置文件
	public static final String CONFIG_FILE = "config_file";
	// 数据源
	public static final String DATASOURCES = "datasources";
	// 是否启用原生SQL
	public static final String SQL = "sql";
	// 是否启用原生语句, 默认false
	public static final String NATIVE = "native";
	// 默认数据源
	public static final String DEFAULT_DATASOURCE = "default_datasource";
	// 表信息配置文件路径
	public static final String TABLE_CONFIG_PATH = "table_config_path";
	// 所有数据库表配置, 该属性项非配置文件配置, 是项目自动生成的属性, 目的是将数据库表的配置信息与项目全局的配置信息放在一个对象里 全局配置
	public static final String TABLE_CONFIG = "table_config";
	// 别名表达式, 参见 JEXL
	public static final String TABLE_ALIAS_EXPRESSION = "table_alias_expression";
	// 关联节点连接符, 如A join B, 在查询结果中的关联节点名称为, COLUMN(A) + connector + B
	public static final String ASSOCIATION_CONNECTOR = "association_connector";
	// 权限策略
	public static final String PERMISSION_POLICY = "permission_policy";
	// 公共列配置
	public static final String COLUMNS = "columns";
	// 元数据刷新策略
	public static final String METADATA_REFRESH_POLICY = "metadata_refresh_policy";
	// 元数据刷新间隔
	public static final String METADATA_REFRESH_INTERVAL = "metadata_refresh_interval";
	// 关联join查询时, 如果2个表有多个字段关联, 在join的时候使用on-and进行多字段关联
	public static final String JOIN_ON_MULTI_COLUMN = "join_on_multi_column";
}
