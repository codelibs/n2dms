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

public class ProfileChat implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean chatEnabled;

    private boolean autoLoginEnabled;

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(final boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public boolean isAutoLoginEnabled() {
        return autoLoginEnabled;
    }

    public void setAutoLoginEnabled(final boolean autoLoginEnabled) {
        this.autoLoginEnabled = autoLoginEnabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("chatEnabled=");
        sb.append(chatEnabled);
        sb.append(", autoLoginEnabled=");
        sb.append(autoLoginEnabled);
        sb.append("}");
        return sb.toString();
    }
}
