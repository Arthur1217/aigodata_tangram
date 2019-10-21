package com.github.mengxianun.core.permission;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.mengxianun.core.App;
import com.github.mengxianun.core.request.Connector;

/**
 * 权限工具类, 基于当前用户
 * 
 * @author mengxiangyun
 *
 */
public class Permissions {

	private Permissions() {
		throw new AssertionError();
	}

	public static boolean hasTablePermission(String table) {
		return hasTablePermission(null, table);
	}

	public static boolean hasTablePermission(String source, String table) {
		return App.hasTablePermissions(source, table);
	}

	public static boolean hasTableSelectPermission(String table) {
		return hasTableActionPermission(table, TableAction.QUERY);
	}

	public static boolean hasTableInsertPermission(String table) {
		return hasTableActionPermission(table, TableAction.ADD);
	}

	public static boolean hasTableUpdatePermission(String table) {
		return hasTableActionPermission(table, TableAction.UPDATE);
	}

	public static boolean hasTableDeletePermission(String table) {
		return hasTableActionPermission(table, TableAction.DELETE);
	}

	private static boolean hasTableActionPermission(String table, TableAction action) {
		if (hasTablePermission(table)) {
			List<TablePermission> tablePermissions = App.getTablePermissions(null, table);
			return tablePermissions.parallelStream()
					.anyMatch(e -> e.action() == TableAction.ALL || e.action() == action);
		}
		return false;
	}

	public static TablePermissions getTablePermissions(String table) {
		List<TablePermission> tablePermissions = App.getTablePermissions(null, table);
		List<ConnectorCondition> conditions = tablePermissions.stream().flatMap(e -> e.conditions().stream())
				.collect(Collectors.toList());
		return new TablePermissions(null, table, conditions);
	}

	public static TablePermissions getTableSelectPermissions(String table) {
		return getTableActionPermissions(table, TableAction.QUERY);
	}

	public static TablePermissions getTableInsertPermissions(String table) {
		return getTableActionPermissions(table, TableAction.ADD);
	}

	public static TablePermissions getTableUpdatePermissions(String table) {
		return getTableActionPermissions(table, TableAction.UPDATE);
	}

	public static TablePermissions getTableDeletePermissions(String table) {
		return getTableActionPermissions(table, TableAction.DELETE);
	}

	public static TablePermissions getTableActionPermissions(String table, TableAction action) {
		List<TablePermission> tablePermissions = App.getTablePermissions(null, table);
		List<ConnectorCondition> conditions = tablePermissions.stream()
				.filter(e -> e.action() == TableAction.ALL || e.action() == action)
				.flatMap(e -> e.conditions().stream())
				.collect(Collectors.toList());
		return new TablePermissions(null, table, conditions);
	}

	public static String getWhere(String table) {
		// to do
		return null;
	}

	/**
	 * 获取可以查询的列
	 * 
	 * @param table
	 * @return
	 */
	public static Set<String> getQueryColumns(String table) {
		// to do
		return null;
	}

	static class TablePermissions {

		private String source;
		private String table;
		private List<ConnectorCondition> tableConditions;

		public TablePermissions(String source, String table, List<ConnectorCondition> tableConditions) {
			super();
			this.source = source;
			this.table = table;
			this.tableConditions = tableConditions;
		}

		public String getSource() {
			return source;
		}

		public String getTable() {
			return table;
		}

		public List<ConnectorCondition> getTableConditions() {
			return tableConditions;
		}

		public String toSQL() {
			if (tableConditions.isEmpty()) {
				return "";
			}
			StringBuilder builder = new StringBuilder();
			for (ConnectorCondition connectorCondition : tableConditions) {
				Connector connector = connectorCondition.connector();
				Condition condition = connectorCondition.condition();

				builder.append(" ").append(connector).append(" ");
				if (condition instanceof TableCondition) {
					// to do
				} else if (condition instanceof ExpressionCondition) {
					builder.append(((ExpressionCondition) condition).expression());
				}
			}
			return builder.toString();
		}

	}

}