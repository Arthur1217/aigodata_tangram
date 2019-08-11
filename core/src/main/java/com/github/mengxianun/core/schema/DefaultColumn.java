package com.github.mengxianun.core.schema;

import com.google.gson.JsonObject;

public class DefaultColumn implements Column {

	private Table table;
	private ColumnType columnType;
	private String name;
	private Boolean nullable;
	private String remarks;
	private Integer columnSize;
	private Column relationColumn;

	private JsonObject info;
	// 自定义配置信息
	private JsonObject config = new JsonObject();

	public DefaultColumn() {
	}

	public DefaultColumn(String name) {
		this.name = name;
	}

	public DefaultColumn(Table table, String name) {
		this.name = name;
		this.table = table;
	}

	public DefaultColumn(Table table, ColumnType columnType, String name) {
		this.name = name;
		this.columnType = columnType;
		this.table = table;
	}

	public DefaultColumn(Table table, ColumnType columnType, String name, Boolean nullable, String remarks,
			Integer columnSize) {
		this.table = table;
		this.columnType = columnType;
		this.name = name;
		this.nullable = nullable;
		this.remarks = remarks;
		this.columnSize = columnSize;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public ColumnType getType() {
		return columnType;
	}

	@Override
	public Boolean isNullable() {
		return nullable;
	}

	@Override
	public String getRemarks() {
		return remarks;
	}

	@Override
	public Integer getColumnSize() {
		return columnSize;
	}

	@Override
	public boolean isPrimaryKey() {
		return false;
	}

	@Override
	public Column getRelationColumn() {
		return relationColumn;
	}

	@Override
	public JsonObject getInfo() {
		if (info != null && info.size() > 0) {
			return info;
		}
		info = new JsonObject();
		info.addProperty("name", name);
		info.addProperty("type", columnType.getName());
		info.addProperty("size", columnSize);
		info.addProperty("remarks", remarks);
		return info;
	}

	@Override
	public JsonObject getConfig() {
		return config;
	}

	public void setNullable(Boolean nullable) {
		this.nullable = nullable;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public void setColumnSize(Integer columnSize) {
		this.columnSize = columnSize;
	}

	public void setRelationColumn(Column relationColumn) {
		this.relationColumn = relationColumn;
	}

	public void setConfig(JsonObject config) {
		this.config = config;
	}

}
