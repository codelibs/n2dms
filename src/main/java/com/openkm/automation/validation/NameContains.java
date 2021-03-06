/**
 *  OpenKM, Open Document Management System (http://www.openkm.com)
 *  Copyright (c) 2006-2015  Paco Avila & Josep Llort
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

package com.openkm.automation.validation;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.automation.AutomationUtils;
import com.openkm.automation.Validation;
import com.openkm.dao.bean.NodeDocument;
import com.openkm.dao.bean.NodeFolder;
import com.openkm.dao.bean.NodeMail;

public class NameContains implements Validation {
    private static Logger log = LoggerFactory.getLogger(NameContains.class);

    @Override
    public boolean isValid(HashMap<String, Object> env, Object... params) {
        String str = AutomationUtils.getString(0, params);
        Object node = AutomationUtils.getNode(env);

        try {
            if (node != null) {
                if (node instanceof NodeDocument) {
                    return ((NodeDocument) node).getName().contains(str);
                } else if (node instanceof NodeFolder) {
                    return ((NodeFolder) node).getName().contains(str);
                } else if (node instanceof NodeMail) {
                    return ((NodeMail) node).getName().contains(str);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
