/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.ConversationService;
import org.gluu.oxtrust.ldap.service.AttributeService;
import org.gluu.oxtrust.ldap.service.JsonConfigurationService;
import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.model.GluuOrganization;
import org.gluu.oxtrust.model.RegistrationConfiguration;
import org.gluu.oxtrust.model.SimpleCustomPropertiesListModel;
import org.gluu.oxtrust.model.Tuple;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.model.GluuAttribute;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.util.StringHelper;
import org.xdi.util.Util;

/**
 * Action class for displaying attributes
 * 
 * @author Yuriy Movchan Date: 10.17.2010
 */
@ConversationScoped
@Named("registrationManagementAction")
public class RegistrationManagementAction implements SimpleCustomPropertiesListModel, Serializable {

	private static final long serialVersionUID = -3832167044333943686L;

	@Inject
	private Logger log;

	@Inject 
	private OrganizationService organizationService;
	
	@Inject
	private AttributeService attributeService;

	@Inject
	private FacesMessages facesMessages;

	@Inject
	private ConversationService conversationService;

	private boolean captchaDisabled;
	
	private boolean configureRegistrationForm;
	
	private List<GluuAttribute> attributes = new ArrayList<GluuAttribute>();
	private List<GluuAttribute> selectedAttributes = new ArrayList<GluuAttribute>();
	
	private String searchPattern;
	
	private String oldSearchPattern;

	@Inject
	private JsonConfigurationService jsonConfigurationService;

	private List<String> customScriptTypes;

	private String attributeName;

	private String attributeData;

	private AppConfiguration oxTrustappConfiguration;
	
	public AppConfiguration getOxTrustappConfiguration() {
		return oxTrustappConfiguration;
	}

	public void setOxTrustappConfiguration(AppConfiguration oxTrustappConfiguration) {
		this.oxTrustappConfiguration = oxTrustappConfiguration;
	}

	public String search() {
		if (StringHelper.isNotEmpty(this.oldSearchPattern) && Util.equals(this.oldSearchPattern, this.searchPattern)) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		try {
		    if (StringHelper.isEmpty(this.searchPattern)) {
	            this.attributes = attributeService.getAllAttributes();
		    } else {
		        this.attributes = attributeService.searchAttributes(this.searchPattern, OxTrustConstants.searchPersonsSizeLimit);
		    }

            for (GluuAttribute selectedAttribute : selectedAttributes) {
                if (!attributes.contains(selectedAttribute)) {
                    attributes.add(selectedAttribute);
                }
            }
			this.oldSearchPattern = this.searchPattern;
		} catch (Exception ex) {
			log.error("Failed to find attributes", ex);
			return OxTrustConstants.RESULT_FAILURE;
		}

		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	public String init(){
		customScriptTypes = new ArrayList<String>();
		customScriptTypes.add(OxTrustConstants.INIT_REGISTRATION_SCRIPT);
		customScriptTypes.add(OxTrustConstants.PRE_REGISTRATION_SCRIPT);
		customScriptTypes.add(OxTrustConstants.POST_REGISTRATION_SCRIPT);
		

		this.oxTrustappConfiguration = jsonConfigurationService.getOxTrustappConfiguration();

		GluuOrganization org = organizationService.getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		if(config != null){
			captchaDisabled = config.isCaptchaDisabled();
			
			List<String> attributeList = config.getAdditionalAttributes();
			if(attributeList != null && ! attributeList.isEmpty()){
				configureRegistrationForm = true;
				for(String attributeInum: attributeList){
					GluuAttribute attribute = attributeService.getAttributeByInum(attributeInum);
					selectedAttributes.add(attribute);
					attributes.add(attribute);
				}
			}

		}
		
		search();

		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	/**
	 * @param linksExpirationFrequency
	 * @return
	 */
	private Tuple<String, String> getPeriod(BigInteger linksExpirationFrequency) {
		Tuple<String, String> result = new Tuple<String, String>();
		BigInteger[] divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(7*24*60));
		BigInteger weeks = divideAndRemainder[0];
		BigInteger reminder = divideAndRemainder[1];
		if( ! weeks.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(weeks.toString());
			result.setValue1("3");
			return result;
		}
		
		divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(24*60));
		BigInteger days = divideAndRemainder[0];
		reminder = divideAndRemainder[1];
		if( ! days.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(days.toString());
			result.setValue1("2");
			return result;
		}
		
		divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(60));
		BigInteger hours = divideAndRemainder[0];
		reminder = divideAndRemainder[1];
		if( ! hours.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(hours.toString());
			result.setValue1("1");
			return result;
		}

		BigInteger minutes = linksExpirationFrequency;
		result.setValue0(minutes.toString());
		result.setValue1("0");
		return result;
	}

	public String save() {
		GluuOrganization org = organizationService.getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		if (config == null) {
			config = new RegistrationConfiguration();
		}

		config.setCaptchaDisabled(captchaDisabled);

		List<String> attributeList = new ArrayList<String>();
		if (configureRegistrationForm) {
			for (GluuAttribute attribute : selectedAttributes) {
				attributeList.add(attribute.getInum());
			}
		}
		config.setAdditionalAttributes(attributeList);
		org.setOxRegistrationConfiguration(config);
		organizationService.updateOrganization(org);

		jsonConfigurationService.saveOxTrustappConfiguration(this.oxTrustappConfiguration);

		facesMessages.add(FacesMessage.SEVERITY_INFO, "Registration configuration updated successfully");

		return OxTrustConstants.RESULT_SUCCESS;
	}

	@Override
	public void removeItemFromSimpleCustomProperties(
			List<SimpleCustomProperty> simpleCustomProperties,
			SimpleCustomProperty simpleCustomProperty) {
		if (simpleCustomProperties != null) {
			simpleCustomProperties.remove(simpleCustomProperty);
		}
		
	}


	@Override
	public void addItemToSimpleCustomProperties(
			List<SimpleCustomProperty> simpleCustomProperties) {
		if (simpleCustomProperties != null) {
			simpleCustomProperties.add(new SimpleCustomProperty("", ""));
		}
	}
	
	public String cancel() {
		facesMessages.add(FacesMessage.SEVERITY_INFO, "Registration configuration not updated");
		conversationService.endConversation();

		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	public String lookupAttributeData(){
		GluuAttribute attribute = attributeService.getAttributeByName(attributeName);
		attributeData = "Uid:\t" +  attributeName;
		attributeData += "<br/>Description:\t" +  attribute.getDescription();
		attributeData += "<br/>Origin:\t" +  attribute.getOrigin();

		return OxTrustConstants.RESULT_SUCCESS;
	}

	public boolean isCaptchaDisabled() {
		return captchaDisabled;
	}

	public void setCaptchaDisabled(boolean captchaDisabled) {
		this.captchaDisabled = captchaDisabled;
	}

	public String getSearchPattern() {
		return searchPattern;
	}

	public void setSearchPattern(String searchPattern) {
		this.searchPattern = searchPattern;
	}

	public boolean isConfigureRegistrationForm() {
		return configureRegistrationForm;
	}

	public void setConfigureRegistrationForm(boolean configureRegistrationForm) {
		this.configureRegistrationForm = configureRegistrationForm;
	}

	public List<GluuAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<GluuAttribute> attributes) {
		this.attributes = attributes;
	}

	public List<GluuAttribute> getSelectedAttributes() {
		return selectedAttributes;
	}

	public void setSelectedAttributes(List<GluuAttribute> selectedAttributes) {
		this.selectedAttributes = selectedAttributes;
	}

	public String getAttributeData() {
		return attributeData;
	}

	public void setAttributeData(String attributeData) {
		this.attributeData = attributeData;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

}
