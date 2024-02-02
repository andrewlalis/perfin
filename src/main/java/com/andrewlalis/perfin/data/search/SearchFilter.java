package com.andrewlalis.perfin.data.search;

import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.Pair;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface SearchFilter {
    String whereClause();
    List<Pair<Integer, Object>> args();
    default List<String> joinClauses() {
        return Collections.emptyList();
    }

    record Impl(String whereClause, List<Pair<Integer, Object>> args, List<String> joinClauses) implements SearchFilter {}

    class Builder {
        private String whereClause;
        private List<Pair<Integer, Object>> args = new ArrayList<>();
        private List<String> joinClauses = new ArrayList<>();

        public Builder where(String clause) {
            this.whereClause = clause;
            return this;
        }

        public Builder withArg(int sqlType, Object value) {
            args.add(new Pair<>(sqlType, value));
            return this;
        }

        public Builder withArg(int value) {
            return withArg(Types.INTEGER, value);
        }

        public Builder withArg(long value) {
            return withArg(Types.BIGINT, value);
        }

        public Builder withArg(String value) {
            return withArg(Types.VARCHAR, value);
        }

        public Builder withArg(LocalDateTime utcTimestamp) {
            return withArg(Types.TIMESTAMP, DbUtil.timestampFromUtcLDT(utcTimestamp));
        }

        public Builder withJoin(String joinClause) {
            joinClauses.add(joinClause);
            return this;
        }

        public SearchFilter build() {
            return new Impl(whereClause, args, joinClauses);
        }
    }
}
