package com.yahoo.covid19.usertypes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

public class StringCollection implements UserType {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int[] sqlTypes() {
        return new int[] {StringType.INSTANCE.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return List.class;
    }

    @Override
    public boolean equals(Object firstObject, Object secondObject)
            throws HibernateException {

        return Objects.equals(firstObject, secondObject);
    }

    @Override
    public int hashCode(Object object)
            throws HibernateException {

        return Objects.hashCode(object);
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException, SQLException {
        if (resultSet.getString(names[0]) != null) {

            // Get the rawJson
            String rawJson = resultSet.getString(names[0]);

            try {
                return MAPPER.readValue(rawJson, new TypeReference<List<String>>(){});
            } catch (IOException e) {
                throw new HibernateException("Could not retrieve an instance of the mapped class from a JDBC resultset. - " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int i, SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException, SQLException {
        if (value == null) {
            preparedStatement.setNull(i, Types.NULL);
        } else {
            try {
                String json = MAPPER.writeValueAsString(value);
                preparedStatement.setString(i, json);
            } catch (JsonProcessingException e) {
                throw new HibernateException("Could not write an instance of the mapped class to a prepared statement.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object deepCopy(Object object)
            throws HibernateException {

        // Since mutable is false, return the object
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable disassemble(Object value)
            throws HibernateException {

        return value == null ? null : (Serializable) deepCopy(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {

        return cached == null ? null : deepCopy(cached);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {

        return deepCopy(original);
    }

}

