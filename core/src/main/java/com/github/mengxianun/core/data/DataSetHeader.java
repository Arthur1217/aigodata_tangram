package com.github.mengxianun.core.data;

import java.util.List;

import com.github.mengxianun.core.item.ColumnItem;

public interface DataSetHeader {

	public List<ColumnItem> getColumnItems();

	public ColumnItem getColumnItem(int index);

	public int size();

}