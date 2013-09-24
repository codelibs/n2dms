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
import java.util.Calendar;

public class CronTab implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String BSH = "application/x-bsh";

    public static final String JAR = "application/x-java-archive";

    private long id;

    private String name;

    private String expression;

    private String fileContent;

    private String fileName;

    private String fileMime;

    private String mail;

    private Calendar lastBegin;

    private Calendar lastEnd;

    private boolean active;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        this.expression = expression;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(final String fileContent) {
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getFileMime() {
        return fileMime;
    }

    public void setFileMime(final String fileMime) {
        this.fileMime = fileMime;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(final String mail) {
        this.mail = mail;
    }

    public Calendar getLastBegin() {
        return lastBegin;
    }

    public void setLastBegin(final Calendar lastBegin) {
        this.lastBegin = lastBegin;
    }

    public Calendar getLastEnd() {
        return lastEnd;
    }

    public void setLastEnd(final Calendar lastEnd) {
        this.lastEnd = lastEnd;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id=");
        sb.append(id);
        sb.append(", name=");
        sb.append(name);
        sb.append(", fileName=");
        sb.append(fileName);
        sb.append(", fileMime=");
        sb.append(fileMime);
        sb.append(", fileContent=");
        sb.append("[BIG]");
        sb.append(", mail=");
        sb.append(mail);
        sb.append(", lastBegin=");
        sb.append(lastBegin == null ? null : lastBegin.getTime());
        sb.append(", lastEnd=");
        sb.append(lastEnd == null ? null : lastEnd.getTime());
        sb.append(", active=");
        sb.append(active);
        sb.append("}");
        return sb.toString();
    }
}