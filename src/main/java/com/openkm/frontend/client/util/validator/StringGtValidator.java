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

package com.openkm.frontend.client.util.validator;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBoxBase;

import eu.maydu.gwt.validation.client.ValidationAction;
import eu.maydu.gwt.validation.client.ValidationResult;
import eu.maydu.gwt.validation.client.ValidationResult.ValidationError;
import eu.maydu.gwt.validation.client.Validator;
import eu.maydu.gwt.validation.client.i18n.StandardValidationMessages;
import eu.maydu.gwt.validation.client.i18n.ValidationMessages;

/**
 * StringGtValidator
 * 
 * @author jllort
 *
 */
public class StringGtValidator extends Validator<StringGtValidator> {

    protected TextBoxBase text;

    protected SuggestBox suggest;

    protected String value;

    public StringGtValidator(final TextBoxBase text) {
        this(text, false);
    }

    public StringGtValidator(final TextBoxBase text, final String value) {
        this(text, value, false);
    }

    public StringGtValidator(final TextBoxBase text,
            final boolean preventsPropagationOfValidationChain) {
        this(text, preventsPropagationOfValidationChain, null);
    }

    public StringGtValidator(final TextBoxBase text,
            final boolean preventsPropagationOfValidationChain,
            final String customMsgKey) {
        super();
        setPreventsPropagationOfValidationChain(preventsPropagationOfValidationChain);
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        this.text = text;
        setCustomMsgKey(customMsgKey);
    }

    public StringGtValidator(final TextBoxBase text, final String value,
            final boolean preventsPropagationOfValidationChain) {
        this(text, value, preventsPropagationOfValidationChain, null);
    }

    public StringGtValidator(final TextBoxBase text, final String value,
            final boolean preventsPropagationOfValidationChain,
            final String customMsgKey) {
        super();
        setPreventsPropagationOfValidationChain(preventsPropagationOfValidationChain);
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        this.text = text;
        setVAlue(value);
        setCustomMsgKey(customMsgKey);
    }

    public StringGtValidator(final SuggestBox suggest) {
        this(suggest, false);
    }

    public StringGtValidator(final SuggestBox suggest,
            final boolean preventsPropagationOfValidationChain) {
        this(suggest, preventsPropagationOfValidationChain, null);
    }

    public StringGtValidator(final SuggestBox suggest,
            final boolean preventsPropagationOfValidationChain,
            final String customMsgKey) {
        super();
        setPreventsPropagationOfValidationChain(preventsPropagationOfValidationChain);
        if (suggest == null) {
            throw new IllegalArgumentException("suggest must not be null");
        }
        this.suggest = suggest;
        setCustomMsgKey(customMsgKey);
    }

    @Override
    public ValidationResult validate(final ValidationMessages allMessages) {
        final StandardValidationMessages messages = allMessages
                .getStandardMessages();
        String str;
        if (text != null) {
            str = text.getText();
        } else {
            str = suggest.getText();
        }
        if (str == null) {
            str = "";
        }
        if (value.compareTo(str) > 0) {
            final ValidationResult result = new ValidationResult();
            final ValidationError error = result.new ValidationError(null,
                    getErrorMessage(allMessages, messages.validator_max()));
            result.getErrors().add(error);
            return result;
        }

        return null;
    }

    @Override
    public void invokeActions(final ValidationResult result) {
        if (text != null) {
            for (final ValidationAction<TextBoxBase> action : getFailureActions()) {
                action.invoke(result, text);
            }
        } else {
            for (final ValidationAction<SuggestBox> action : getFailureActions()) {
                action.invoke(result, suggest);
            }
        }
    }

    public StringGtValidator setVAlue(final String value) {
        this.value = value;
        return this;
    }
}