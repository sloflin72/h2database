/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import java.sql.SQLException;

import org.h2.engine.Session;
import org.h2.schema.Schema;

/**
 * This class represents a non-transaction statement that involves a schema.
 */
public abstract class SchemaCommand extends DefineCommand {

    private final Schema schema;

    public SchemaCommand(Session session, Schema schema) {
        super(session);
        this.schema = schema;
    }

    protected Schema getSchema() throws SQLException {
        return schema;
    }

}
