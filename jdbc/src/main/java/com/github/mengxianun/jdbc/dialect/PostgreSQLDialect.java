package com.github.mengxianun.jdbc.dialect;

import com.github.mengxianun.core.Dialect;
import com.github.mengxianun.jdbc.JdbcDataContext;
import com.google.auto.service.AutoService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@AutoService(Dialect.class)
public class PostgreSQLDialect extends JdbcDialect {

	public PostgreSQLDialect(JdbcDataContext jdbcDataContext) {
		super(jdbcDataContext);
	}

	@Override
	public String getType() {
		return "postgresql";
	}

	@Override
	public String getJsonPlaceholder() {
		return "?::json";
	}

	@Override
	public JsonElement getJsonValue(Object value) {
		JsonElement jsonValue = super.getJsonValue(value);
		if (jsonValue.isJsonObject()) {
			String stringValue = jsonValue.getAsJsonObject().get("value").getAsString();
			return new JsonParser().parse(stringValue);
		}
		return jsonValue;
	}

	@Override
	protected String getTimeLikeColumn(String column) {
		return column + "::text";
	}

}
