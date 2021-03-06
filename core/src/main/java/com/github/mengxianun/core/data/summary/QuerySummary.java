package com.github.mengxianun.core.data.summary;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.mengxianun.core.Action;
import com.github.mengxianun.core.config.ResultAttributes;
import com.github.mengxianun.core.data.AbstractSummary;
import com.github.mengxianun.core.data.DefaultHeader;
import com.github.mengxianun.core.data.Header;
import com.github.mengxianun.core.data.Row;
import com.github.mengxianun.core.item.LimitItem;

public abstract class QuerySummary extends AbstractSummary {

	protected Header header;
	private List<Row> rows;
	private int index;

	protected long total;
	protected List<Map<String, Object>> values;

	public QuerySummary(Action action, List<Map<String, Object>> values) {
		this(action, values, -1);
	}

	public QuerySummary(Action action, List<Map<String, Object>> values, long total) {
		super(action, values);
		if (action != null) {
			this.header = new DefaultHeader(action.getColumnItems());
		}
		this.values = values;
		this.total = total;
	}

	public Row getRow() {
		return getRows().get(index++);
	}

	public List<Row> getRows() {
		if (rows == null) {
			rows = toRows();
		}
		return rows;
	}

	public abstract List<Row> toRows();

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public List<Map<String, Object>> getValues() {
		if (values == null) {
			values = toValues();
		}
		return values;
	}

	public void setValues(List<Map<String, Object>> values) {
		this.values = values;
	}

	public abstract List<Map<String, Object>> toValues();

	@Override
	public Object getData() {
		Object data = getValues();
		if (action == null) {
			return values;
		}
		if (action.isDetail()) {
			data = values.isEmpty() ? Collections.emptyMap() : values.get(0);
		}
		if (action.isLimit()) {
			LimitItem limitItem = action.getLimitItem();
			long start = limitItem.getStart();
			long end = limitItem.getEnd();
			Map<String, Object> pageResult = new LinkedHashMap<>();
			pageResult.put(ResultAttributes.START, start);
			pageResult.put(ResultAttributes.END, end);
			pageResult.put(ResultAttributes.TOTAL, total);
			pageResult.put(ResultAttributes.DATA, values);
			data = pageResult;
		}
		return data;
	}

}
