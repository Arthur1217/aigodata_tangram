package com.github.mengxianun.core.permission;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TablePermission {

	public static TablePermission create(String table) {
		return create(null, table);
	}

	public static TablePermission create(@Nullable String source, String table) {
		return create(source, table, TableAction.ALL);
	}

	public static TablePermission create(@Nullable String source, String table, TableAction action) {
		return create(source, table, null, action, Collections.emptyList());
	}

	public static TablePermission create(@Nullable String source, String table, @Nullable Object id, TableAction action,
			List<Condition> conditions) {
		return new AutoValue_TablePermission.Builder().source(source).table(table).id(id).action(action)
				.conditions(conditions).build();
	}

	@Nullable
	public abstract String source();

	public abstract String table();

	@Nullable
	public abstract Object id();

	public abstract TableAction action();

	public abstract List<Condition> conditions();

	public static Builder builder() {
		return new AutoValue_TablePermission.Builder().conditions(Collections.emptyList());
	}

	@AutoValue.Builder
	public abstract static class Builder {

		public abstract Builder source(String source);

		public abstract Builder table(String table);

		public abstract Builder id(Object id);

		public Builder action(String action) {
			return action(TableAction.from(action));
		}

		public abstract Builder action(TableAction action);

		abstract Optional<TableAction> action();

		public abstract Builder conditions(List<Condition> conditions);

		abstract TablePermission autoBuild();

		public TablePermission build() {
			if (!action().isPresent()) {
				action(TableAction.ALL);
			}
			return autoBuild();
		}
	}

}
