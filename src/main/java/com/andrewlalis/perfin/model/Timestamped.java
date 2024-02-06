package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.data.util.DbUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public interface Timestamped {
    /**
     * Gets the timestamp at which the entity was created, in UTC timezone.
     * @return The UTC timestamp at which this entity was created.
     */
    LocalDateTime getTimestamp();

    record Stub(long id, LocalDateTime timestamp) implements Timestamped {
        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public static Stub fromResultSet(ResultSet rs) throws SQLException {
            return new Stub(rs.getLong(1), DbUtil.utcLDTFromTimestamp(rs.getTimestamp(2)));
        }
    }
}
