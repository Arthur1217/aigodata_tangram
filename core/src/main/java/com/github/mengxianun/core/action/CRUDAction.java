package com.github.mengxianun.core.action;

import java.util.ArrayList;
import java.util.List;

import com.github.mengxianun.core.DataContext;
import com.github.mengxianun.core.SQLBuilder;
import com.github.mengxianun.core.data.Summary;
import com.github.mengxianun.core.item.ColumnItem;
import com.github.mengxianun.core.item.FilterItem;
import com.github.mengxianun.core.item.GroupItem;
import com.github.mengxianun.core.item.JoinItem;
import com.github.mengxianun.core.item.LimitItem;
import com.github.mengxianun.core.item.OrderItem;
import com.github.mengxianun.core.item.TableItem;
import com.github.mengxianun.core.item.ValueItem;
import com.github.mengxianun.core.request.FileType;
import com.github.mengxianun.core.request.Operation;
import com.github.mengxianun.core.request.Template;
import com.github.mengxianun.core.schema.Table;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;

@Deprecated
public class CRUDAction extends AbstractAction {

	private JsonObject requestData;
	private Operation operation;
	private List<TableItem> tableItems;
	private List<ColumnItem> columnItems;
	private List<JoinItem> joinItems;
	private List<FilterItem> filterItems;
	private List<GroupItem> groupItems;
	private List<OrderItem> orderItems;
	private LimitItem limitItem;
	private List<List<ValueItem>> insertValueItems;
	private List<ValueItem> updateValueItem;
	private String file;
	private Template template;
	private String nativeSQL;
	private String nativeContent;
	private SQLBuilder sqlBuilder;
	private boolean distinct;

	private final List<Table> tables;
	private final List<Table> joinTables;
	// 是否处理 Join Limit 的情况
	private boolean handleJoinLimit;

	public CRUDAction(DataContext dataContext) {
		super(dataContext);
		this.tableItems = new ArrayList<>();
		this.columnItems = new ArrayList<>();
		this.joinItems = new ArrayList<>();
		this.filterItems = new ArrayList<>();
		this.groupItems = new ArrayList<>();
		this.orderItems = new ArrayList<>();
		this.insertValueItems = new ArrayList<>();
		this.handleJoinLimit = true;
		this.tables = new ArrayList<>();
		this.joinTables = new ArrayList<>();
	}

	public void addTableItem(TableItem tableItem) {
		if (tableItem == null) {
			return;
		}
		this.tableItems.add(tableItem);
	}

	public void addTableItems(List<TableItem> tableItems) {
		if (tableItems == null || tableItems.isEmpty()) {
			return;
		}
		this.tableItems.addAll(tableItems);
	}

	public void addColumnItem(ColumnItem columnItem) {
		if (columnItem == null) {
			return;
		}
		for (ColumnItem existColumnItem : columnItems) {
			if (existColumnItem.getColumn() == columnItem.getColumn()
					&& (existColumnItem.getAlias() != null && existColumnItem.getAlias().equals(columnItem.getAlias()))
					&& (existColumnItem.getExpression() != null
							&& existColumnItem.getExpression().equals(columnItem.getExpression()))) {
				return;
			}
		}
		this.columnItems.add(columnItem);
	}

	public void addColumnItems(List<ColumnItem> columnItems) {
		if (columnItems == null || columnItems.isEmpty()) {
			return;
		}
		columnItems.forEach(this::addColumnItem);
	}

	public void addJoinItem(JoinItem joinItem) {
		if (joinItem == null) {
			return;
		}
		this.joinItems.add(joinItem);
	}

	public void addJoinItems(List<JoinItem> joinItems) {
		if (joinItems == null || joinItems.isEmpty()) {
			return;
		}
		this.joinItems.addAll(joinItems);
	}

	public void addFilterItem(FilterItem filterItem) {
		if (filterItem == null) {
			return;
		}
		this.filterItems.add(filterItem);
	}

	public void addFilterItem(List<FilterItem> filterItems) {
		if (filterItems == null || filterItems.isEmpty()) {
			return;
		}
		this.filterItems.addAll(filterItems);
	}

	public void addGroupItem(GroupItem groupItem) {
		if (groupItem == null) {
			return;
		}
		this.groupItems.add(groupItem);
	}

	public void addGroupItems(List<GroupItem> groupItems) {
		if (groupItems == null || groupItems.isEmpty()) {
			return;
		}
		this.groupItems.addAll(groupItems);
	}

	public void addOrderItem(OrderItem orderItem) {
		if (orderItem == null) {
			return;
		}
		this.orderItems.add(orderItem);
	}

	public void addOrderItems(List<OrderItem> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}
		this.orderItems.addAll(orderItems);
	}

	public void addLimitItem(LimitItem limitItem) {
		if (limitItem == null) {
			return;
		}
		this.limitItem = limitItem;
	}

	public void addInsertValueItems(List<ValueItem> valueItems) {
		if (valueItems == null || valueItems.isEmpty()) {
			return;
		}
		this.insertValueItems.add(valueItems);
	}

	public void addAllInsertValueItems(List<List<ValueItem>> valueItems) {
		if (valueItems == null || valueItems.isEmpty()) {
			return;
		}
		this.insertValueItems.addAll(valueItems);
	}

	public void addUpdateValueItem(ValueItem valueItem) {
		if (valueItem == null) {
			return;
		}
		this.updateValueItem.add(valueItem);
	}

	public void addAllUpdateValueItem(List<ValueItem> valueItems) {
		if (valueItems == null || valueItems.isEmpty()) {
			return;
		}
		this.updateValueItem.addAll(valueItems);
	}

	public void addTable(Table table) {
		this.tables.add(table);
	}

	public void addJoinTable(Table join) {
		this.joinTables.add(join);
	}

	public boolean isDetail() {
		return operation != null && operation == Operation.DETAIL;
	}

	public boolean isSelect() {
		return operation != null && (operation == Operation.SELECT);
	}

	public boolean isQuery() {
		return isDetail() || isSelect();
	}

	public boolean isUpdate() {
		return operation != null && operation == Operation.UPDATE;
	}

	public boolean isInsert() {
		return operation != null && operation == Operation.INSERT;
	}

	public boolean isDelete() {
		return operation != null && operation == Operation.DELETE;
	}

	public boolean isCRUD() {
		return isQuery() || isInsert() || isUpdate() || isDelete();
	}

	public boolean isTransaction() {
		return operation != null && operation == Operation.TRANSACTION;
	}

	public boolean isStruct() {
		return operation != null && operation == Operation.STRUCT;
	}

	public boolean isStructs() {
		return operation != null && operation == Operation.STRUCTS;
	}

	public boolean isSQL() {
		return operation != null && operation == Operation.SQL;
	}

	public boolean isNative() {
		return operation != null && operation == Operation.NATIVE;
	}

	public boolean isFile() {
		return !Strings.isNullOrEmpty(file);
	}

	public boolean isTemplate() {
		return template != null;
	}

	public boolean isJoin() {
		return !joinItems.isEmpty();
	}

	public boolean isGroup() {
		return !groupItems.isEmpty();
	}

	public boolean isLimit() {
		return limitItem != null;
	}

	public boolean isJoinTable(Table join) {
		return joinTables.contains(join);
	}

	public TableItem getPrimaryTableItem() {
		return tableItems.get(0);
	}

	public Table getPrimaryTable() {
		return getPrimaryTableItem().getTable();
	}

	public List<Table> getJoinTables() {
		return joinTables;
	}

	public String getFilename() {
		if (Strings.isNullOrEmpty(file)) {
			return "";
		}
		if (file.contains(".")) {
			return file;
		} else {
			return getPrimaryTable().getDisplayName() + "." + file;
		}
	}

	public FileType getFileType() {
		if (Strings.isNullOrEmpty(file)) {
			return null;
		}
		String fileTypeString = file;
		if (file.contains(".")) {
			fileTypeString = file.split("\\.")[1];
		}
		return FileType.from(fileTypeString);
	}

	public CRUDAction count() {
		CRUDAction count = new CRUDAction(dataContext);
		count.setOperation(Operation.DETAIL);
		count.build();
		String countSql = sqlBuilder.countSql();
		List<Object> countParams = sqlBuilder.countParams();
		SQLBuilder countSqlBuilder = count.getSqlBuilder();
		countSqlBuilder.setSql(countSql);
		countSqlBuilder.setParams(countParams);
		return count;
	}

	public void build() {
		if (sqlBuilder == null) {
			//			sqlBuilder = dataContext.getSQLBuilder(this);
		}
		sqlBuilder.toSql();
	}

	public void reBuild() {
		sqlBuilder.clear();
		sqlBuilder.toSql();
	}

	public String getSql() {
		return sqlBuilder.getSql();
	}

	public List<Object> getParams() {
		return sqlBuilder.getParams();
	}

	public JsonObject getRequestData() {
		return requestData;
	}

	public void setRequestData(JsonObject requestData) {
		this.requestData = requestData;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public List<TableItem> getTableItems() {
		return tableItems;
	}

	public void setTableItems(List<TableItem> tableItems) {
		this.tableItems = tableItems;
	}

	public List<ColumnItem> getColumnItems() {
		return columnItems;
	}

	public void setColumnItems(List<ColumnItem> columnItems) {
		this.columnItems = columnItems;
	}

	public List<JoinItem> getJoinItems() {
		return joinItems;
	}

	public void setJoinItems(List<JoinItem> joinItems) {
		this.joinItems = joinItems;
	}

	public List<FilterItem> getFilterItems() {
		return filterItems;
	}

	public void setFilterItems(List<FilterItem> filterItems) {
		this.filterItems = filterItems;
	}

	public List<GroupItem> getGroupItems() {
		return groupItems;
	}

	public void setGroupItems(List<GroupItem> groupItems) {
		this.groupItems = groupItems;
	}

	public List<OrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}

	public LimitItem getLimitItem() {
		return limitItem;
	}

	public void setLimitItem(LimitItem limitItem) {
		this.limitItem = limitItem;
	}

	public List<List<ValueItem>> getInsertValueItems() {
		return insertValueItems;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	public String getNativeSQL() {
		return nativeSQL;
	}

	public void setNativeSQL(String nativeSQL) {
		this.nativeSQL = nativeSQL;
	}

	public String getNativeContent() {
		return nativeContent;
	}

	public void setNativeContent(String nativeContent) {
		this.nativeContent = nativeContent;
	}

	public SQLBuilder getSqlBuilder() {
		return sqlBuilder;
	}

	public void setSqlBuilder(SQLBuilder sqlBuilder) {
		this.sqlBuilder = sqlBuilder;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isHandleJoinLimit() {
		return handleJoinLimit;
	}

	public void setHandleJoinLimit(boolean handleJoinLimit) {
		this.handleJoinLimit = handleJoinLimit;
	}

	@Override
	public Summary execute() {
		// TODO Auto-generated method stub
		return null;
	}

}
