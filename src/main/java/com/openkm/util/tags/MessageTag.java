/**
 * OpenKM, Open Document Management System (http://www.openkm.com)
 * Copyright (c) 2006-2013 Paco Avila & Josep Llort
 * 
 * No bytes were intentionally harmed during the development of this application.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.openkm.util.tags;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import com.openkm.core.DatabaseException;
import com.openkm.dao.LanguageDAO;
import com.openkm.dao.bean.Language;

@SuppressWarnings("serial")
public class MessageTag extends TagSupport {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String module;

    private String key;

    @Override
    public int doStartTag() {
        try {
            final Locale locale = pageContext.getRequest().getLocale();
            final String lang = locale.getLanguage() + "-"
                    + locale.getCountry();
            String msg = LanguageDAO.getTranslation(lang, module, key);
            if (msg == null || msg.equals("")) {
                msg = LanguageDAO.getTranslation(Language.DEFAULT, module, key);
            }
            pageContext.getOut().write(msg);
        } catch (final DatabaseException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return Tag.SKIP_BODY;
    }

    @Override
    public void release() {
        super.release();
        setModule("");
        setKey("");
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getModule() {
        return module;
    }

    public void setModule(final String module) {
        this.module = module;
    }
}