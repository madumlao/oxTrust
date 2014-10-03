/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.model.scim;

/**
 * SCIM Entitlements
 * 
 * @author Reda Zerrad Date: 04.23.2012
 */
public class ScimEntitlements {

	private String value;

	public ScimEntitlements() {
		value = "";
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
