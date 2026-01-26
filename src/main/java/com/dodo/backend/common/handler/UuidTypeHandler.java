package com.dodo.backend.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
        ps.setBytes(i, uuidToBytes(parameter));
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return bytesToUuid(rs.getBytes(columnName));
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return bytesToUuid(rs.getBytes(columnIndex));
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return bytesToUuid(cs.getBytes(columnIndex));
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private UUID bytesToUuid(byte[] bytes) {
        if (bytes == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}