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

public class ProfileToolbar implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean createFolderVisible;

    private boolean findFolderVisible;

    private boolean findDocumentVisible;

    private boolean downloadVisible;

    private boolean downloadPdfVisible;

    private boolean lockVisible;

    private boolean unlockVisible;

    private boolean addDocumentVisible;

    private boolean checkoutVisible;

    private boolean checkinVisible;

    private boolean cancelCheckoutVisible;

    private boolean deleteVisible;

    private boolean addPropertyGroupVisible;

    private boolean removePropertyGroupVisible;

    private boolean startWorkflowVisible;

    private boolean addSubscriptionVisible;

    private boolean removeSubscriptionVisible;

    private boolean refreshVisible;

    private boolean homeVisible;

    private boolean scannerVisible;

    private boolean uploaderVisible;

    private boolean omrVisible;

    public boolean isCreateFolderVisible() {
        return createFolderVisible;
    }

    public void setCreateFolderVisible(final boolean createFolderVisible) {
        this.createFolderVisible = createFolderVisible;
    }

    public boolean isFindFolderVisible() {
        return findFolderVisible;
    }

    public void setFindFolderVisible(final boolean findFolderVisible) {
        this.findFolderVisible = findFolderVisible;
    }

    public boolean isFindDocumentVisible() {
        return findDocumentVisible;
    }

    public void setFindDocumentVisible(final boolean findDocumentVisible) {
        this.findDocumentVisible = findDocumentVisible;
    }

    public boolean isDownloadVisible() {
        return downloadVisible;
    }

    public void setDownloadVisible(final boolean downloadVisible) {
        this.downloadVisible = downloadVisible;
    }

    public boolean isDownloadPdfVisible() {
        return downloadPdfVisible;
    }

    public void setDownloadPdfVisible(final boolean downloadPdfVisible) {
        this.downloadPdfVisible = downloadPdfVisible;
    }

    public boolean isLockVisible() {
        return lockVisible;
    }

    public void setLockVisible(final boolean lockVisible) {
        this.lockVisible = lockVisible;
    }

    public boolean isUnlockVisible() {
        return unlockVisible;
    }

    public void setUnlockVisible(final boolean unlockVisible) {
        this.unlockVisible = unlockVisible;
    }

    public boolean isAddDocumentVisible() {
        return addDocumentVisible;
    }

    public void setAddDocumentVisible(final boolean addDocumentVisible) {
        this.addDocumentVisible = addDocumentVisible;
    }

    public boolean isCheckoutVisible() {
        return checkoutVisible;
    }

    public void setCheckoutVisible(final boolean checkoutVisible) {
        this.checkoutVisible = checkoutVisible;
    }

    public boolean isCheckinVisible() {
        return checkinVisible;
    }

    public void setCheckinVisible(final boolean checkinVisible) {
        this.checkinVisible = checkinVisible;
    }

    public boolean isCancelCheckoutVisible() {
        return cancelCheckoutVisible;
    }

    public void setCancelCheckoutVisible(final boolean cancelCheckoutVisible) {
        this.cancelCheckoutVisible = cancelCheckoutVisible;
    }

    public boolean isDeleteVisible() {
        return deleteVisible;
    }

    public void setDeleteVisible(final boolean deleteVisible) {
        this.deleteVisible = deleteVisible;
    }

    public boolean isAddPropertyGroupVisible() {
        return addPropertyGroupVisible;
    }

    public void setAddPropertyGroupVisible(final boolean addPropertyGroupVisible) {
        this.addPropertyGroupVisible = addPropertyGroupVisible;
    }

    public boolean isRemovePropertyGroupVisible() {
        return removePropertyGroupVisible;
    }

    public void setRemovePropertyGroupVisible(
            final boolean removePropertyGroupVisible) {
        this.removePropertyGroupVisible = removePropertyGroupVisible;
    }

    public boolean isStartWorkflowVisible() {
        return startWorkflowVisible;
    }

    public void setStartWorkflowVisible(final boolean startWorkflowVisible) {
        this.startWorkflowVisible = startWorkflowVisible;
    }

    public boolean isAddSubscriptionVisible() {
        return addSubscriptionVisible;
    }

    public void setAddSubscriptionVisible(final boolean addSubscriptionVisible) {
        this.addSubscriptionVisible = addSubscriptionVisible;
    }

    public boolean isRemoveSubscriptionVisible() {
        return removeSubscriptionVisible;
    }

    public void setRemoveSubscriptionVisible(
            final boolean removeSubscriptionVisible) {
        this.removeSubscriptionVisible = removeSubscriptionVisible;
    }

    public boolean isRefreshVisible() {
        return refreshVisible;
    }

    public void setRefreshVisible(final boolean refreshVisible) {
        this.refreshVisible = refreshVisible;
    }

    public boolean isHomeVisible() {
        return homeVisible;
    }

    public void setHomeVisible(final boolean homeVisible) {
        this.homeVisible = homeVisible;
    }

    public boolean isScannerVisible() {
        return scannerVisible;
    }

    public void setScannerVisible(final boolean scannerVisible) {
        this.scannerVisible = scannerVisible;
    }

    public boolean isUploaderVisible() {
        return uploaderVisible;
    }

    public void setUploaderVisible(final boolean uploaderVisible) {
        this.uploaderVisible = uploaderVisible;
    }

    public boolean isOmrVisible() {
        return omrVisible;
    }

    public void setOmrVisible(final boolean omrVisible) {
        this.omrVisible = omrVisible;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("createFolderVisible=");
        sb.append(createFolderVisible);
        sb.append(", findFolderVisible=");
        sb.append(findFolderVisible);
        sb.append(", findDocumentVisible=");
        sb.append(findDocumentVisible);
        sb.append(", downloadVisible=");
        sb.append(downloadVisible);
        sb.append(", downloadPdfVisible=");
        sb.append(downloadPdfVisible);
        sb.append(", addDocumentVisible=");
        sb.append(addDocumentVisible);
        sb.append(", lockVisible=");
        sb.append(lockVisible);
        sb.append(", unlockVisible=");
        sb.append(unlockVisible);
        sb.append(", checkinVisible=");
        sb.append(checkinVisible);
        sb.append(", checkoutVisible=");
        sb.append(checkoutVisible);
        sb.append(", cancelCheckoutVisible=");
        sb.append(cancelCheckoutVisible);
        sb.append(", deleteVisible=");
        sb.append(deleteVisible);
        sb.append(", addPropertyGroupVisible=");
        sb.append(addPropertyGroupVisible);
        sb.append(", removePropertyGroupVisible=");
        sb.append(removePropertyGroupVisible);
        sb.append(", startWorkflowVisible=");
        sb.append(startWorkflowVisible);
        sb.append(", addSubscriptionVisible=");
        sb.append(addSubscriptionVisible);
        sb.append(", removeSubscriptionVisible=");
        sb.append(removeSubscriptionVisible);
        sb.append(", homeVisible=");
        sb.append(homeVisible);
        sb.append(", refreshVisible=");
        sb.append(refreshVisible);
        sb.append(", scannerVisible=");
        sb.append(scannerVisible);
        sb.append(", uploaderVisible=");
        sb.append(uploaderVisible);
        sb.append(", omrVisible=");
        sb.append(omrVisible);
        sb.append("}");
        return sb.toString();
    }
}