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

package com.openkm.frontend.client.widget.filebrowser.menu;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.openkm.frontend.client.Main;
import com.openkm.frontend.client.bean.GWTAvailableOption;
import com.openkm.frontend.client.bean.ToolBarOption;
import com.openkm.frontend.client.util.Util;
import com.openkm.frontend.client.widget.MenuBase;

/**
 * Browser menu
 * 
 * @author jllort
 *
 */
public class TaxonomyMenu extends MenuBase {

    private ToolBarOption toolBarOption;

    private MenuBar dirMenu;

    private MenuItem checkout;

    private MenuItem checkin;

    private MenuItem cancelCheckout;

    private MenuItem delete;

    private MenuItem rename;

    private MenuItem download;

    private MenuItem lock;

    private MenuItem unlock;

    private MenuItem move;

    private MenuItem copy;

    private MenuItem note;

    private MenuItem category;

    private MenuItem keyword;

    private MenuItem propertyGroup;

    private MenuItem bookmark;

    private MenuItem home;

    private MenuItem export;

    /**
     * Browser menu
     */
    public TaxonomyMenu() {
        toolBarOption = new ToolBarOption();
        // The item selected must be called on style.css : .okm-MenuBar .gwt-MenuItem-selected

        // First initialize language values
        dirMenu = new MenuBar(true);
        download = new MenuItem(Util.menuHTML("img/icon/actions/download.gif",
                Main.i18n("filebrowser.menu.download")), true, downloadFile);
        download.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(download);
        checkout = new MenuItem(Util.menuHTML("img/icon/actions/checkout.gif",
                Main.i18n("filebrowser.menu.checkout")), true, checkoutFile);
        checkout.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(checkout);
        checkin = new MenuItem(Util.menuHTML("img/icon/actions/checkin.gif",
                Main.i18n("filebrowser.menu.checkin")), true, checkinFile);
        checkin.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(checkin);
        cancelCheckout = new MenuItem(Util.menuHTML(
                "img/icon/actions/cancel_checkout.gif",
                Main.i18n("filebrowser.menu.checkout.cancel")), true,
                cancelCheckinFile);
        cancelCheckout.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(cancelCheckout);
        lock = new MenuItem(Util.menuHTML("img/icon/actions/lock.gif",
                Main.i18n("filebrowser.menu.lock")), true, lockFile);
        lock.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(lock);
        unlock = new MenuItem(Util.menuHTML("img/icon/actions/unlock.gif",
                Main.i18n("filebrowser.menu.unlock")), true, unlockFile);
        unlock.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(unlock);
        delete = new MenuItem(Util.menuHTML("img/icon/actions/delete.gif",
                Main.i18n("filebrowser.menu.delete")), true, deleteFile);
        delete.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(delete);
        rename = new MenuItem(Util.menuHTML("img/icon/actions/rename.gif",
                Main.i18n("general.menu.edit.rename")), true, renameFile);
        rename.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(rename);
        move = new MenuItem(Util.menuHTML("img/icon/actions/move_document.gif",
                Main.i18n("general.menu.edit.move")), true, moveFile);
        move.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(move);
        copy = new MenuItem(Util.menuHTML("img/icon/actions/copy.gif",
                Main.i18n("general.menu.edit.copy")), true, copyFile);
        copy.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(copy);
        note = new MenuItem(Util.menuHTML("img/icon/actions/add_note.png",
                Main.i18n("general.menu.edit.add.note")), true, addNote);
        note.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(note);
        category = new MenuItem(
                Util.menuHTML("img/icon/stackpanel/table_key.gif",
                        Main.i18n("category.add")), true, addCategory);
        category.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(category);
        keyword = new MenuItem(Util.menuHTML("img/icon/actions/book_add.png",
                Main.i18n("keyword.add")), true, addKeyword);
        keyword.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(keyword);
        propertyGroup = new MenuItem(Util.menuHTML(
                "img/icon/actions/add_property_group.gif",
                Main.i18n("general.menu.edit.add.property.group")), true,
                addPropertyGroup);
        propertyGroup.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(propertyGroup);
        bookmark = new MenuItem(Util.menuHTML(
                "img/icon/actions/add_bookmark.gif",
                Main.i18n("general.menu.bookmark.add")), true, addBookmark);
        bookmark.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(bookmark);
        home = new MenuItem(Util.menuHTML("img/icon/actions/bookmark.gif",
                Main.i18n("filebrowser.menu.set.home")), true, setHome);
        home.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(home);
        export = new MenuItem(Util.menuHTML("img/icon/actions/export.gif",
                Main.i18n("general.menu.file.export")), true, exportToFile);
        export.addStyleName("okm-MenuItem-strike");
        dirMenu.addItem(export);

        dirMenu.setStyleName("okm-MenuBar");
        initWidget(dirMenu);
    }

    // Command menu to download file
    Command downloadFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.downloadOption) {
                Main.get().mainPanel.topPanel.toolBar.executeDownload();
                hide();
            }
        }
    };

    // Command menu to checkout file
    Command checkoutFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.checkoutOption) {
                Main.get().mainPanel.topPanel.toolBar.executeCheckout();
                hide();
            }
        }
    };

    // Command menu to checkin file
    Command checkinFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.checkinOption) {
                Main.get().mainPanel.topPanel.toolBar.executeCheckin();
                hide();
            }
        }
    };

    // Command menu to cancel checkin file
    Command cancelCheckinFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.cancelCheckoutOption) {
                Main.get().mainPanel.topPanel.toolBar.executeCancelCheckout();
                hide();
            }
        }
    };

    // Command menu to lock file
    Command lockFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.lockOption) {
                Main.get().mainPanel.topPanel.toolBar.executeLock();
                hide();
            }
        }
    };

    // Command menu to unlock file
    Command unlockFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.unLockOption) {
                Main.get().mainPanel.topPanel.toolBar.executeUnlock();
                hide();
            }
        }
    };

    // Command menu to lock file
    Command deleteFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.deleteOption) {
                Main.get().mainPanel.topPanel.toolBar.executeDelete();
                hide();
            }
        }
    };

    // Command menu to rename file
    Command renameFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.renameOption) {
                Main.get().mainPanel.topPanel.toolBar.executeRename();
                hide();
            }
        }
    };

    // Command menu to rename file
    Command moveFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.moveOption) {
                if (Main.get().mainPanel.desktop.browser.fileBrowser
                        .isMassive()) {
                    Main.get().activeFolderTree.massiveMove();
                } else {
                    Main.get().mainPanel.desktop.browser.fileBrowser.move();
                }
                hide();
            }
        }
    };

    // Command menu to copy file
    Command copyFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.copyOption) {
                if (Main.get().mainPanel.desktop.browser.fileBrowser
                        .isMassive()) {
                    Main.get().activeFolderTree.massiveCopy();
                } else {
                    Main.get().mainPanel.desktop.browser.fileBrowser.copy();
                }
                hide();
            }
        }
    };

    // Command menu to add note
    Command addNote = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.addNoteOption) {
                Main.get().mainPanel.topPanel.toolBar.addNote();
                hide();
            }
        }
    };

    // Command menu to add category
    Command addCategory = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.addCategoryOption) {
                Main.get().mainPanel.topPanel.toolBar.addCategory();
                hide();
            }
        }
    };

    // Command menu to add category
    Command addKeyword = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.addKeywordOption) {
                Main.get().mainPanel.topPanel.toolBar.addKeyword();
                hide();
            }
        }
    };

    // Command menu to add property group
    Command addPropertyGroup = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.addPropertyGroupOption) {
                Main.get().mainPanel.topPanel.toolBar.addPropertyGroup();
                hide();
            }
        }
    };

    // Command menu to add bookmark
    Command addBookmark = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.bookmarkOption) {
                Main.get().mainPanel.topPanel.toolBar.executeAddBookmark();
                hide();
            }
        }
    };

    // Command menu to set default home
    Command setHome = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.homeOption) {
                Main.get().mainPanel.desktop.browser.fileBrowser.setHome();
                hide();
            }
        }
    };

    // Command menu to set default home
    Command exportToFile = new Command() {
        @Override
        public void execute() {
            if (toolBarOption.exportOption) {
                Main.get().mainPanel.desktop.browser.fileBrowser
                        .exportFolderToFile();
                hide();
            }
        }
    };

    @Override
    public void langRefresh() {
        checkout.setHTML(Util.menuHTML("img/icon/actions/checkout.gif",
                Main.i18n("filebrowser.menu.checkout")));
        checkin.setHTML(Util.menuHTML("img/icon/actions/checkin.gif",
                Main.i18n("filebrowser.menu.checkin")));
        delete.setHTML(Util.menuHTML("img/icon/actions/delete.gif",
                Main.i18n("filebrowser.menu.delete")));
        rename.setHTML(Util.menuHTML("img/icon/actions/rename.gif",
                Main.i18n("general.menu.edit.rename")));
        cancelCheckout.setHTML(Util.menuHTML(
                "img/icon/actions/cancel_checkout.gif",
                Main.i18n("filebrowser.menu.checkout.cancel")));
        lock.setHTML(Util.menuHTML("img/icon/actions/lock.gif",
                Main.i18n("filebrowser.menu.lock")));
        unlock.setHTML(Util.menuHTML("img/icon/actions/unlock.gif",
                Main.i18n("filebrowser.menu.unlock")));
        download.setHTML(Util.menuHTML("img/icon/actions/download.gif",
                Main.i18n("filebrowser.menu.download")));
        move.setHTML(Util.menuHTML("img/icon/actions/move_document.gif",
                Main.i18n("general.menu.edit.move")));
        copy.setHTML(Util.menuHTML("img/icon/actions/copy.gif",
                Main.i18n("general.menu.edit.copy")));
        note.setHTML(Util.menuHTML("img/icon/actions/add_note.png",
                Main.i18n("general.menu.edit.add.note")));
        category.setHTML(Util.menuHTML("img/icon/stackpanel/table_key.gif",
                Main.i18n("category.add")));
        keyword.setHTML(Util.menuHTML("img/icon/actions/book_add.png",
                Main.i18n("keyword.add")));
        propertyGroup.setHTML(Util.menuHTML(
                "img/icon/actions/add_property_group.gif",
                Main.i18n("general.menu.edit.add.property.group")));
        bookmark.setHTML(Util.menuHTML("img/icon/actions/add_bookmark.gif",
                Main.i18n("general.menu.bookmark.add")));
        home.setHTML(Util.menuHTML("img/icon/actions/bookmark.gif",
                Main.i18n("filebrowser.menu.set.home")));
        export.setHTML(Util.menuHTML("img/icon/actions/export.gif",
                Main.i18n("general.menu.file.export")));
    }

    @Override
    public void setOptions(final ToolBarOption toolBarOption) {
        this.toolBarOption = toolBarOption;
        evaluateMenuOptions();
    }

    @Override
    public void disableAllOptions() {
        toolBarOption = new ToolBarOption();
        evaluateMenuOptions();
    }

    @Override
    public void evaluateMenuOptions() {
        if (toolBarOption.downloadOption) {
            enable(download);
        } else {
            disable(download);
        }
        if (toolBarOption.deleteOption) {
            enable(delete);
        } else {
            disable(delete);
        }
        if (toolBarOption.renameOption) {
            enable(rename);
        } else {
            disable(rename);
        }
        if (toolBarOption.checkoutOption) {
            enable(checkout);
        } else {
            disable(checkout);
        }
        if (toolBarOption.checkinOption) {
            enable(checkin);
        } else {
            disable(checkin);
        }
        if (toolBarOption.cancelCheckoutOption) {
            enable(cancelCheckout);
        } else {
            disable(cancelCheckout);
        }
        if (toolBarOption.lockOption) {
            enable(lock);
        } else {
            disable(lock);
        }
        if (toolBarOption.unLockOption) {
            enable(unlock);
        } else {
            disable(unlock);
        }
        if (toolBarOption.moveOption) {
            enable(move);
        } else {
            disable(move);
        }
        if (toolBarOption.copyOption) {
            enable(copy);
        } else {
            disable(copy);
        }
        if (toolBarOption.addNoteOption) {
            enable(note);
        } else {
            disable(note);
        }
        if (toolBarOption.addCategoryOption) {
            enable(category);
        } else {
            disable(category);
        }
        if (toolBarOption.addKeywordOption) {
            enable(keyword);
        } else {
            disable(keyword);
        }
        if (toolBarOption.addPropertyGroupOption) {
            enable(propertyGroup);
        } else {
            disable(propertyGroup);
        }
        if (toolBarOption.bookmarkOption) {
            enable(bookmark);
        } else {
            disable(bookmark);
        }
        if (toolBarOption.homeOption) {
            enable(home);
        } else {
            disable(home);
        }
        if (toolBarOption.exportOption) {
            enable(export);
        } else {
            disable(export);
        }
        if (toolBarOption.bookmarkOption) {
            enable(bookmark);
        } else {
            disable(bookmark);
        }
    }

    @Override
    public void setAvailableOption(final GWTAvailableOption option) {
        if (!option.isDownloadOption()) {
            dirMenu.removeItem(download);
        }
        if (!option.isDeleteOption()) {
            dirMenu.removeItem(delete);
        }
        if (!option.isRenameOption()) {
            dirMenu.removeItem(rename);
        }
        if (!option.isCheckoutOption()) {
            dirMenu.removeItem(checkout);
        }
        if (!option.isCheckinOption()) {
            dirMenu.removeItem(checkin);
        }
        if (!option.isCancelCheckoutOption()) {
            dirMenu.removeItem(cancelCheckout);
        }
        if (!option.isLockOption()) {
            dirMenu.removeItem(lock);
        }
        if (!option.isUnLockOption()) {
            dirMenu.removeItem(unlock);
        }
        if (!option.isMoveOption()) {
            dirMenu.removeItem(move);
        }
        if (!option.isCopyOption()) {
            dirMenu.removeItem(copy);
        }
        if (!option.isAddNoteOption()) {
            dirMenu.removeItem(note);
        }
        if (!option.isAddCategoryOption()) {
            dirMenu.removeItem(category);
        }
        if (!option.isAddKeywordOption()) {
            dirMenu.removeItem(keyword);
        }
        if (!option.isAddPropertyGroupOption()) {
            dirMenu.removeItem(propertyGroup);
        }
        if (!option.isAddBookmarkOption()) {
            dirMenu.removeItem(bookmark);
        }
        if (!option.isSetHomeOption()) {
            dirMenu.removeItem(home);
        }
        if (!option.isExportOption()) {
            dirMenu.removeItem(export);
        }
    }

    @Override
    public void enableAddPropertyGroup() {
        if (dirMenu != null) { // Condition caused by loading case
            toolBarOption.addPropertyGroupOption = true;
        }
        enable(propertyGroup);
    }

    @Override
    public void disableAddPropertyGroup() {
        if (dirMenu != null) { // Condition caused by loading case
            toolBarOption.addPropertyGroupOption = false;
        }
        disable(propertyGroup);
    }

    /**
     * Hide popup menu
     */
    public void hide() {
        Main.get().mainPanel.desktop.browser.fileBrowser.taxonomyMenuPopup
                .hide();
    }
}
