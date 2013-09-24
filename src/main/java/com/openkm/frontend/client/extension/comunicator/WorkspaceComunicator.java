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

package com.openkm.frontend.client.extension.comunicator;

import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTWorkspace;
import com.openkm.frontend.client.extension.widget.tabworkspace.TabWorkspaceExtension;

/**
 * WorkspaceComunicator
 * 
 * @author jllort
 *
 */
public class WorkspaceComunicator {

    /**
     * SelectedTab
     */
    public static int getSelectedTab() {
        return Main.get().mainPanel.topPanel.tabWorkspace.getSelectedTab();
    }

    /**
     * ChangeSelectedTab
     */
    public static void changeSelectedTab(final int selectedTab) {
        Main.get().mainPanel.topPanel.tabWorkspace
                .changeSelectedTab(selectedTab);
    }

    /**
     * SelectedWorkspace
     */
    public static int getSelectedWorkspace() {
        return Main.get().mainPanel.topPanel.tabWorkspace
                .getSelectedWorkspace();
    }

    /**
     * Workspace
     */
    public static GWTWorkspace getWorkspace() {
        return Main.get().workspaceUserProperties.getWorkspace();
    }

    /**
     * TabExtensionIndex
     */
    public static int getTabExtensionIndex(final TabWorkspaceExtension widget) {
        return Main.get().mainPanel.topPanel.tabWorkspace
                .getTabExtensionIndex(widget);
    }
}