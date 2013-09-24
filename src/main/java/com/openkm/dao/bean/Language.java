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

package com.openkm.dao.bean;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Language
 * 
 * @author jllort
 */
public class Language implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT = "en-GB";

    private String id;

    private String name;

    private String imageContent;

    private String imageMime;

    private Set<Translation> translations = new HashSet<Translation>();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(final Set<Translation> translations) {
        this.translations = translations;
    }

    public String getImageContent() {
        return imageContent;
    }

    public void setImageContent(final String imageContent) {
        this.imageContent = imageContent;
    }

    public String getImageMime() {
        return imageMime;
    }

    public void setImageMime(final String imageMime) {
        this.imageMime = imageMime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id=");
        sb.append(id);
        sb.append(", name=");
        sb.append(name);
        sb.append(", imageMime=");
        sb.append(imageMime);
        sb.append(", imageContent=");
        sb.append("[BIG]");
        sb.append(", translations=");
        sb.append(translations);
        sb.append("}");
        return sb.toString();
    }
}