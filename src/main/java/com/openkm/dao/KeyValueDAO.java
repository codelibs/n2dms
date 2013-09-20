/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2013  Paco Avila & Josep Llort
 *
 *  No bytes were intentionally harmed during the development of this application.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.DatabaseException;
import com.openkm.dao.bean.KeyValue;
import com.openkm.util.DatabaseMetadataUtils;

public class KeyValueDAO {
    private static Logger log = LoggerFactory.getLogger(KeyValueDAO.class);

    private KeyValueDAO() {
    }

    /**
     * Find key values
     */
    public static List<KeyValue> getKeyValues(final String query)
            throws DatabaseException {
        log.debug("getKeyValues({})", query);
        final List<KeyValue> ret = new ArrayList<KeyValue>();
        Session session = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            final Query q = session.createQuery(query);

            for (final Object obj : q.list()) {
                if (obj instanceof Object[]) {
                    final Object[] ao = (Object[]) obj;
                    final KeyValue kv = new KeyValue();
                    kv.setKey(String.valueOf(ao[0]));
                    kv.setValue(String.valueOf(ao[1]));
                    ret.add(kv);
                }
            }

            log.debug("getKeyValues: {}", ret);
            return ret;
        } catch (final HibernateException e) {
            throw new DatabaseException(e.getMessage(), e);
        } finally {
            HibernateUtil.close(session);
        }
    }

    /**
     * Find key values
     */
    public static List<KeyValue> getKeyValues(final List<String> tables,
            final String query) throws DatabaseException {
        log.debug("getKeyValues({}, {})", tables, query);
        final String realQuery = DatabaseMetadataUtils.replaceVirtual(tables,
                query);
        final List<KeyValue> ret = getKeyValues(realQuery);
        log.debug("getKeyValues: {}", ret);
        return ret;
    }

    /**
     * Find key values
     */
    public static List<KeyValue> getKeyValues(final String table,
            final String query) throws DatabaseException {
        log.debug("getKeyValues({}, {})", table, query);
        final List<String> tables = new ArrayList<String>();
        tables.add(table);
        final List<KeyValue> ret = getKeyValues(tables, query);
        log.debug("getKeyValues: {}", ret);
        return ret;
    }
}
