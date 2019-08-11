package com.github.mengxianun.core;

import java.util.List;
import java.util.Set;

import com.github.mengxianun.core.config.AssociationType;
import com.github.mengxianun.core.data.Summary;
import com.github.mengxianun.core.data.summary.MultiSummary;
import com.github.mengxianun.core.schema.Column;
import com.github.mengxianun.core.schema.Relationship;
import com.github.mengxianun.core.schema.Schema;
import com.github.mengxianun.core.schema.Table;

public interface DataContext {

	public Summary execute(Action action);

	public MultiSummary execute(Action... actions);

	public Summary executeSql(String sql);

	public Summary executeNative(String statement);

	public List<Schema> getSchemas();

	public Schema getDefaultSchema();

	public Schema getSchema(String schemaName);

	/**
	 * 根据表名查询数据表
	 * 
	 * @param tableName
	 *            Config中配置的表别名或者实际的表名
	 * @return Table
	 */
	public Table getTable(String tableName);

	public Table getTable(String schemaName, String tableName);

	public Column getColumn(String tableName, String columnName);

	public Column getColumn(String schemaName, String tableName, String columnName);

	public Dialect getDialect();

	public SQLBuilder getSQLBuilder(Action action);

	public void destroy();

	public void addRelationship(Column primaryColumn, Column foreignColumn, AssociationType associationType);

	/**
	 * 获取主外表的关联关系.
	 * <li>如 A join B, 获取 A 对 B 的关联关系, 将会得到 [ A-B ]
	 * <li>如 A join B join C, 获取 A 对 C 的关联关系, 将会得到 [ A-B, B-C ]
	 * <li>如果没有找到关联关系, 返回一个空集合
	 * 
	 * @param primaryTable
	 * @param foreignTable
	 * @return 主表到外表的多级关系
	 */
	public Set<Relationship> getRelationships(Table primaryTable, Table foreignTable);

	/**
	 * 获取2个表的关联关系类型. 多层关联的情况下, 以收尾的关系为最终关系. 如 A 一对多 B, B 多对一 C, 则 A 一对一 C
	 * 
	 * @param primaryTable
	 * @param foreignTable
	 * @return AssociationType
	 */
	public AssociationType getAssociationType(Table primaryTable, Table foreignTable);

}
