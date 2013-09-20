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

package com.openkm.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

import com.openkm.core.Config;

/**
 * @author pavila
 * 
 */
public class FilenameTokenizer extends CharTokenizer {

    /** Construct a new FilenameTokenizer. */
    public FilenameTokenizer(final Reader in) {
        super(Config.LUCENE_VERSION, in);
    }

    @Override
    protected boolean isTokenChar(final char c) {
        return Character.isLetterOrDigit(c);
    }

    @Override
    protected char normalize(final char c) {
        return Character.toLowerCase(c);
    }
}
