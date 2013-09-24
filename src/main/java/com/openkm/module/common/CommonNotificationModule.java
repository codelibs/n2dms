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

package com.openkm.module.common;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.httpclient.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.openkm.core.AccessDeniedException;
import com.openkm.core.Config;
import com.openkm.core.DatabaseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.TwitterAccountDAO;
import com.openkm.dao.bean.TwitterAccount;
import com.openkm.util.MailUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.TemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class CommonNotificationModule {
    private static Logger log = LoggerFactory
            .getLogger(CommonNotificationModule.class);

    /**
     * Clean preview cache for this document
     */
    public static void sendNotification(final String user,
            final String nodePath, final String from, final List<String> to,
            final String message, final boolean attachment)
            throws TemplateException, MessagingException,
            PathNotFoundException, AccessDeniedException, RepositoryException,
            DatabaseException, IOException {
        log.debug("sendNotification({}, {}, {}, {}, {}, {})", new Object[] {
                user, nodePath, from, to, message, attachment });
        final StringWriter swSubject = new StringWriter();
        final StringWriter swBody = new StringWriter();
        final Configuration cfg = TemplateUtils.getConfig();

        final Map<String, String> model = new HashMap<String, String>();
        model.put("documentUrl", Config.APPLICATION_URL + "?docPath="
                + URLEncoder.encode(nodePath, "UTF-8"));
        model.put("documentPath", nodePath);
        model.put("documentName", PathUtils.getName(nodePath));
        model.put("userId", user);
        model.put("notificationMessage", message);

        if (TemplateUtils.templateExists(Config.NOTIFICATION_MESSAGE_SUBJECT)) {
            final Template tpl = cfg
                    .getTemplate(Config.NOTIFICATION_MESSAGE_SUBJECT);
            tpl.process(model, swSubject);
        } else {
            final StringReader sr = new StringReader(
                    Config.NOTIFICATION_MESSAGE_SUBJECT);
            final Template tpl = new Template("NotificationMessageSubject", sr,
                    cfg);
            tpl.process(model, swSubject);
            sr.close();
        }

        if (TemplateUtils.templateExists(Config.NOTIFICATION_MESSAGE_BODY)) {
            final Template tpl = cfg
                    .getTemplate(Config.NOTIFICATION_MESSAGE_BODY);
            tpl.process(model, swBody);
        } else {
            final StringReader sr = new StringReader(
                    Config.NOTIFICATION_MESSAGE_BODY);
            final Template tpl = new Template("NotificationMessageBody", sr,
                    cfg);
            tpl.process(model, swBody);
            sr.close();
        }

        if (attachment) {
            MailUtils.sendDocument(from, to, swSubject.toString(),
                    swBody.toString(), nodePath);
        } else {
            MailUtils.sendMessage(from, to, swSubject.toString(),
                    swBody.toString());
        }
    }

    /**
     * Send mail subscription message
     */
    public static void sendMailSubscription(final String user,
            final String nodePath, final String eventType, String comment,
            final Set<String> mails) throws TemplateException,
            MessagingException, IOException {
        log.debug("sendMailSubscription({}, {}, {}, {}, {})", new Object[] {
                user, nodePath, eventType, comment, mails });

        if (comment == null) {
            comment = "";
        }

        final StringWriter swSubject = new StringWriter();
        final StringWriter swBody = new StringWriter();
        final Configuration cfg = TemplateUtils.getConfig();

        final Map<String, String> model = new HashMap<String, String>();
        model.put("documentUrl", Config.APPLICATION_URL + "?docPath="
                + URLEncoder.encode(nodePath, "UTF-8"));
        model.put("documentPath", nodePath);
        model.put("documentName", PathUtils.getName(nodePath));
        model.put("userId", user);
        model.put("eventType", eventType);
        model.put("subscriptionComment", comment);

        if (TemplateUtils.templateExists(Config.SUBSCRIPTION_MESSAGE_SUBJECT)) {
            final Template tpl = cfg
                    .getTemplate(Config.SUBSCRIPTION_MESSAGE_SUBJECT);
            tpl.process(model, swSubject);
        } else {
            final StringReader sr = new StringReader(
                    Config.SUBSCRIPTION_MESSAGE_SUBJECT);
            final Template tpl = new Template("SubscriptionMessageSubject", sr,
                    cfg);
            tpl.process(model, swSubject);
            sr.close();
        }

        if (TemplateUtils.templateExists(Config.SUBSCRIPTION_MESSAGE_BODY)) {
            final Template tpl = cfg
                    .getTemplate(Config.SUBSCRIPTION_MESSAGE_BODY);
            tpl.process(model, swBody);
        } else {
            final StringReader sr = new StringReader(
                    Config.SUBSCRIPTION_MESSAGE_BODY);
            final Template tpl = new Template("SubscriptionMessageBody", sr,
                    cfg);
            tpl.process(model, swBody);
            sr.close();
        }

        MailUtils.sendMessage(mails, swSubject.toString(), swBody.toString());
    }

    /**
     * Send twitter subscription message
     */
    public static void sendTwitterSubscription(final String user,
            final String nodePath, final String eventType,
            final String comment, final Set<String> users)
            throws TemplateException, TwitterException, DatabaseException,
            HttpException, IOException {
        log.debug("sendTwitterSubscription({}, {}, {}, {}, {})", new Object[] {
                user, nodePath, eventType, comment, users });
        final Twitter twitter = new Twitter(Config.SUBSCRIPTION_TWITTER_USER,
                Config.SUBSCRIPTION_TWITTER_PASSWORD);
        final StringWriter swStatus = new StringWriter();
        final Configuration cfg = TemplateUtils.getConfig();

        final Map<String, String> model = new HashMap<String, String>();
        model.put(
                "documentUrl",
                MailUtils.getTinyUrl(Config.APPLICATION_URL + "?docPath="
                        + nodePath));
        model.put("documentPath", nodePath);
        model.put("documentName", PathUtils.getName(nodePath));
        model.put("userId", user);
        model.put("eventType", eventType);
        model.put("subscriptionComment", comment);

        if (TemplateUtils.templateExists(Config.SUBSCRIPTION_TWITTER_STATUS)) {
            final Template tpl = cfg
                    .getTemplate(Config.SUBSCRIPTION_TWITTER_STATUS);
            tpl.process(model, swStatus);
        } else {
            final StringReader sr = new StringReader(
                    Config.SUBSCRIPTION_TWITTER_STATUS);
            final Template tpl = new Template("SubscriptionTwitterStatus", sr,
                    cfg);
            tpl.process(model, swStatus);
            sr.close();
        }

        for (final String itUser : users) {
            final Collection<TwitterAccount> twitterAccounts = TwitterAccountDAO
                    .findByUser(itUser, true);

            for (final TwitterAccount ta : twitterAccounts) {
                log.info(
                        "Twitter Notify from {} to {} ({}) - {}",
                        new Object[] { twitter.getUserId(),
                                ta.getTwitterUser(), itUser,
                                swStatus.toString() });
                twitter.sendDirectMessage(ta.getTwitterUser(),
                        swStatus.toString());
            }
        }
    }
}