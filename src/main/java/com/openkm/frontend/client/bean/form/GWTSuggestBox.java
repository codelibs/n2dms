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

package com.openkm.frontend.client.bean.form;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GWTSuggestBox
 * 
 * @author jllort
 *
 */
public class GWTSuggestBox extends GWTFormElement implements IsSerializable {
    private List<GWTValidator> validators = new ArrayList<GWTValidator>();

    private String value = "";

    private String text = ""; // text Value

    private String data = "";

    private boolean readonly = false;

    private String table = "";

    private String dialogTitle = "";

    private String filterQuery = "";

    private String valueQuery = "";

    private int filterMinLen = 0;

    public GWTSuggestBox clone() {
        final GWTSuggestBox newSuggestBox = new GWTSuggestBox();
        newSuggestBox.setData(data);
        newSuggestBox.setDialogTitle(dialogTitle);
        newSuggestBox.setFilterMinLen(filterMinLen);
        newSuggestBox.setFilterQuery(filterQuery);
        newSuggestBox.setHeight(height);
        newSuggestBox.setLabel(label);
        newSuggestBox.setName(name);
        newSuggestBox.setReadonly(readonly);
        newSuggestBox.setTable(table);
        newSuggestBox.setValidators(validators);
        newSuggestBox.setValue(value);
        newSuggestBox.setValueQuery(valueQuery);
        newSuggestBox.setWidth(width);
        return newSuggestBox;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public List<GWTValidator> getValidators() {
        return validators;
    }

    public void setValidators(final List<GWTValidator> validators) {
        this.validators = validators;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(final String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public String getValueQuery() {
        return valueQuery;
    }

    public void setValueQuery(final String valueQuery) {
        this.valueQuery = valueQuery;
    }

    public String getTable() {
        return table;
    }

    public void setTable(final String table) {
        this.table = table;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(final String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public int getFilterMinLen() {
        return filterMinLen;
    }

    public void setFilterMinLen(final int filterMinLen) {
        this.filterMinLen = filterMinLen;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("label=");
        sb.append(label);
        sb.append(", name=");
        sb.append(name);
        sb.append(", value=");
        sb.append(value);
        sb.append(", text=");
        sb.append(text);
        sb.append(", width=");
        sb.append(width);
        sb.append(", height=");
        sb.append(height);
        sb.append(", readonly=");
        sb.append(readonly);
        sb.append(", table=");
        sb.append(table);
        sb.append(", dialogTitle=");
        sb.append(dialogTitle);
        sb.append(", filterQuery=");
        sb.append(filterQuery);
        sb.append(", valueQuery=");
        sb.append(valueQuery);
        sb.append(", validators=");
        sb.append(validators);
        sb.append(", data=");
        sb.append(data);
        sb.append("}");
        return sb.toString();
    }
}