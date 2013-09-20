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

package com.openkm.extractor;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PresentationMLContentHandler extends MsOffice2007ContentHandler {

    @Override
    public String getFilePattern() {
        return "ppt/slides/slide";
    }

    @Override
    public void startElement(final String namespaceURI, final String localName,
            final String rawName, final Attributes atts) throws SAXException {
        if (rawName.equals("a:t")) {
            appendChar = true;
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        if (appendChar) {
            content.append(ch, start, length);
        }
    }

    @Override
    public void endElement(final String namespaceURI, final String localName,
            final String qName) throws SAXException {
        if (qName.equals("a:p")) {
            content.append("\n");
        }
        appendChar = false;
    }
}
