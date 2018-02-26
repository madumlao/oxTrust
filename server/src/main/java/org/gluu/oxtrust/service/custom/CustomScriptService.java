/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service.custom;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.xdi.service.custom.script.AbstractCustomScriptService;

/**
 * Operations with custom scripts
 *
 * @author Yuriy Movchan Date: 12/03/2014
 */
@Stateless
@Named
public class CustomScriptService extends AbstractCustomScriptService{
	
	@Inject
	private OrganizationService organizationService;

	private static final long serialVersionUID = -5283102477313448031L;

    public String baseDn() {
		String orgDn = organizationService.getDnForOrganization();
		return String.format("ou=scripts,%s", orgDn);
    }

}
