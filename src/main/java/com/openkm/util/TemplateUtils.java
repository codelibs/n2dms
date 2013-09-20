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

package com.openkm.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.core.Config;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateUtils {
    private static Logger log = LoggerFactory.getLogger(Config.class);

    private static Configuration cfg = null;

    /**
     * Singleton FreeMaker configuration
     */
    public static synchronized Configuration getConfig() {
        if (cfg == null) {
            try {
                cfg = new Configuration();
                cfg.setDirectoryForTemplateLoading(new File(Config.HOME_DIR));
                cfg.setObjectWrapper(new DefaultObjectWrapper());
            } catch (final IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return cfg;
    }

    /**
     * Check for template existence
     */
    public static boolean templateExists(final String name) {
        try {
            getConfig().getTemplate(name);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * Quick replace utility function
     */
    public static String replace(final String name, final String template,
            final Map<String, Object> model) throws IOException,
            TemplateException {
        final StringReader sr = new StringReader(template);
        final Template tpl = new Template(name, sr, cfg);
        final StringWriter sw = new StringWriter();
        tpl.process(model, sw);
        sw.close();
        sr.close();
        return sw.toString();
    }

    /**
     * Quick replace utility function
     */
    public static void replace(final String name, final InputStream input,
            final Map<String, Object> model, final OutputStream out)
            throws IOException, TemplateException {
        final InputStreamReader isr = new InputStreamReader(input);
        final Template tpl = new Template(name, isr, cfg);
        final OutputStreamWriter osw = new OutputStreamWriter(out);
        tpl.process(model, osw);
        osw.close();
        isr.close();
    }
}
