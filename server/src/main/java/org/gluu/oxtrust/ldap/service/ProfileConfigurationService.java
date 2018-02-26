/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.ldap.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.gluu.oxtrust.config.ConfigurationFactory;
import org.gluu.oxtrust.model.GluuSAMLTrustRelationship;
import org.gluu.oxtrust.model.ProfileConfiguration;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.service.XmlService;
import org.xdi.util.StringHelper;
import org.xdi.util.exception.InvalidConfigurationException;
import org.xdi.util.io.FileUploadWrapper;
import org.xml.sax.SAXException;
import static org.gluu.oxtrust.ldap.service.Shibboleth3ConfService.SHIB3_IDP_METADATA_FOLDER;

/**
 * Provides operations with metadata filters
 * 
 */
@Stateless
@Named
public class ProfileConfigurationService implements Serializable {

	private static final long serialVersionUID = -4691360522345319673L;

	private static final String SHIBBOLETH_SSO = "ShibbolethSSO";
	private static final String SAML1_ARTIFACT_RESOLUTION = "SAML1ArtifactResolution";
	private static final String SAML1_ATTRIBUTE_QUERY = "SAML1AttributeQuery";
	private static final String SAML2_SSO = "SAML2SSO";
	private static final String SAML2_ARTIFACT_RESOLUTION = "SAML2ArtifactResolution";
	private static final String SAML2_ATTRIBUTE_QUERY = "SAML2AttributeQuery";

	@Inject
	private Logger log;

	@Inject
	private TemplateService templateService;

	@Inject
	private XmlService xmlService;
	
	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private AppConfiguration appConfiguration;

	public List<ProfileConfiguration> getAvailableProfileConfigurations() {
		String idpTemplatesLocation = configurationFactory.getIDPTemplatesLocation();
		// File profileConfigurationFolder = new File(configurationFactory.DIR + "shibboleth3" + File.separator + "idp" + File.separator + "ProfileConfiguration");
		File profileConfigurationFolder = new File(idpTemplatesLocation + "shibboleth3" + File.separator + "idp" + File.separator + "ProfileConfiguration");

		File[] profileConfigurationTemplates = null;
		List<ProfileConfiguration> profileConfigurations = new ArrayList<ProfileConfiguration>();

		if (profileConfigurationFolder.exists() && profileConfigurationFolder.isDirectory()) {
			profileConfigurationTemplates = profileConfigurationFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith("ProfileConfiguration.xml.vm");
				}
			});
			for (File profileConfigurationTemplate : profileConfigurationTemplates) {
				profileConfigurations.add(createProfileConfiguration(profileConfigurationTemplate.getName().split("ProfileConfiguration")[0]));
			}
		}
		
		return profileConfigurations;
	}

	private ProfileConfiguration createProfileConfiguration(String profileConfigurationName) {
		ProfileConfiguration profileConfiguration = new ProfileConfiguration();
		profileConfiguration.setName(profileConfigurationName);
		if (SHIBBOLETH_SSO.equals(profileConfigurationName)) {
			profileConfiguration.setIncludeAttributeStatement(false);
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML1_ARTIFACT_RESOLUTION.equals(profileConfigurationName)) {
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML1_ATTRIBUTE_QUERY.equals(profileConfigurationName)) {
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML2_SSO.equals(profileConfigurationName)) {
			profileConfiguration.setIncludeAttributeStatement(true);
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setAssertionProxyCount(0);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		if (SAML2_ARTIFACT_RESOLUTION.equals(profileConfigurationName)) {
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		if (SAML2_ATTRIBUTE_QUERY.equals(profileConfigurationName)) {
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setAssertionProxyCount(0);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		return profileConfiguration;
	}

	public List<ProfileConfiguration> getProfileConfigurationsList(GluuSAMLTrustRelationship trustRelationship) {
		List<ProfileConfiguration> profileConfigurations = new ArrayList<ProfileConfiguration>();
		for (String profileConfigurationName : trustRelationship.getProfileConfigurations().keySet()) {
			profileConfigurations.add(trustRelationship.getProfileConfigurations().get(profileConfigurationName));
		}
		return profileConfigurations;
	}

	public void parseProfileConfigurations(GluuSAMLTrustRelationship trustRelationship) throws SAXException, IOException,
			ParserConfigurationException, FactoryConfigurationError, XPathExpressionException {
		if (trustRelationship.getGluuProfileConfiguration() != null) {
			for (String profileConfigurationXML : trustRelationship.getGluuProfileConfiguration()) {
				Document xmlDocument = xmlService.getXmlDocument(profileConfigurationXML.getBytes(), true);
				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SHIBBOLETH_SSO)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SHIBBOLETH_SSO);

					profileConfiguration.setIncludeAttributeStatement(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("includeAttributeStatement").getNodeValue()));
					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SHIBBOLETH_SSO, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML1_ARTIFACT_RESOLUTION)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML1_ARTIFACT_RESOLUTION);

					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML1_ARTIFACT_RESOLUTION, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML1_ATTRIBUTE_QUERY)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML1_ATTRIBUTE_QUERY);

					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML1_ATTRIBUTE_QUERY, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_SSO)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_SSO);

					profileConfiguration.setIncludeAttributeStatement(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("includeAttributeStatement").getNodeValue()));
					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setAssertionProxyCount(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionProxyCount").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptAssertions")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_SSO, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_ARTIFACT_RESOLUTION)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_ARTIFACT_RESOLUTION);

					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_ARTIFACT_RESOLUTION, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_ATTRIBUTE_QUERY)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_ATTRIBUTE_QUERY);

					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setAssertionProxyCount(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionProxyCount").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_ATTRIBUTE_QUERY, profileConfiguration);
					continue;
				}

			}
		}

	}

	public boolean isProfileConfigurationPresent(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		if(trustRelationship.getProfileConfigurations().keySet().contains(profileConfiguration.getName())){
			ProfileConfiguration storedConfiguration = trustRelationship.getProfileConfigurations().get(profileConfiguration.getName());
			return profileConfiguration.equals(storedConfiguration);
		}
		return false;
	}

	public void updateProfileConfiguration(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		trustRelationship.getProfileConfigurations().put(profileConfiguration.getName(), profileConfiguration);

	}

	public void removeProfileConfiguration(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		trustRelationship.getProfileConfigurations().remove(profileConfiguration.getName());

	}

	public void saveProfileConfigurations(GluuSAMLTrustRelationship trustRelationship, Map<String, FileUploadWrapper> fileWrappers) {
		VelocityContext context = new VelocityContext();

		if (trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO);
			context.put(SHIBBOLETH_SSO + "IncludeAttributeStatement", profileConfiguration.isIncludeAttributeStatement());
			context.put(SHIBBOLETH_SSO + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SHIBBOLETH_SSO + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SHIBBOLETH_SSO + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SHIBBOLETH_SSO + "SignRequests", profileConfiguration.getSignRequests());

			saveCertificate(trustRelationship, fileWrappers, SHIBBOLETH_SSO);
			String certName = trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SHIBBOLETH_SSO + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION);
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignRequests", profileConfiguration.getSignRequests());
			saveCertificate(trustRelationship, fileWrappers, SAML1_ARTIFACT_RESOLUTION);
			String certName = trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION)
					.getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML1_ARTIFACT_RESOLUTION + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY);
			context.put(SAML1_ATTRIBUTE_QUERY + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignRequests", profileConfiguration.getSignRequests());
			saveCertificate(trustRelationship, fileWrappers, SAML1_ATTRIBUTE_QUERY);
			String certName = trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML1_ATTRIBUTE_QUERY + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_SSO) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_SSO);
			context.put(SAML2_SSO + "IncludeAttributeStatement", profileConfiguration.isIncludeAttributeStatement());
			context.put(SAML2_SSO + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML2_SSO + "AssertionProxyCount", profileConfiguration.getAssertionProxyCount());
			context.put(SAML2_SSO + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_SSO + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_SSO + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_SSO + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			context.put(SAML2_SSO + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			saveCertificate(trustRelationship, fileWrappers, SAML2_SSO);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_SSO).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_SSO + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION);
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_ARTIFACT_RESOLUTION + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			context.put(SAML2_ARTIFACT_RESOLUTION + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			saveCertificate(trustRelationship, fileWrappers, SAML2_ARTIFACT_RESOLUTION);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION)
					.getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_ARTIFACT_RESOLUTION + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY);
			context.put(SAML2_ATTRIBUTE_QUERY + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML2_ATTRIBUTE_QUERY + "AssertionProxyCount", profileConfiguration.getAssertionProxyCount());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_ATTRIBUTE_QUERY + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			context.put(SAML2_ATTRIBUTE_QUERY + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			saveCertificate(trustRelationship, fileWrappers, SAML2_ATTRIBUTE_QUERY);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_ATTRIBUTE_QUERY + "SigningCredentialRef", certName);
			}
		}
		
		if(! trustRelationship.getProfileConfigurations().isEmpty()){
			trustRelationship.setGluuProfileConfiguration(new ArrayList<String>());
	
			for (String profileConfigurationName : trustRelationship.getProfileConfigurations().keySet()) {
				trustRelationship.getGluuProfileConfiguration().add(
						templateService.generateConfFile(profileConfigurationName + "ProfileConfiguration.xml", context));
			}
		}else{
			trustRelationship.setGluuProfileConfiguration(null);
		}

	}

	private void saveCertificate(GluuSAMLTrustRelationship trustRelationship, Map<String, FileUploadWrapper> fileWrappers, String name) {
		if (fileWrappers.get(name) != null && fileWrappers.get(name).getStream() != null) {
			String profileConfigurationCertFileName = StringHelper.removePunctuation(name + trustRelationship.getInum());
			saveProfileConfigurationCert(profileConfigurationCertFileName, fileWrappers.get(name).getStream());
			trustRelationship.getProfileConfigurations().get(name)
					.setProfileConfigurationCertFileName(StringHelper.removePunctuation(profileConfigurationCertFileName));
		}

	}

	public String saveProfileConfigurationCert(String profileConfigurationCertFileName, InputStream stream) {

		if (appConfiguration.getShibboleth3IdpRootDir() == null) {
			IOUtils.closeQuietly(stream);
			throw new InvalidConfigurationException("Failed to save Profile Configuration file due to undefined IDP root folder");
		}

		String idpMetadataFolder = appConfiguration.getShibboleth3IdpRootDir() + File.separator + SHIB3_IDP_METADATA_FOLDER + File.separator + "credentials" + File.separator;
		File filterCertFile = new File(idpMetadataFolder + profileConfigurationCertFileName);

		FileOutputStream os = null;
		try {
			os = FileUtils.openOutputStream(filterCertFile);
			IOUtils.copy(stream, os);
			os.flush();
		} catch (IOException ex) {
			log.error("Failed to write  Profile Configuration  certificate file '{}'", filterCertFile, ex);
			ex.printStackTrace();
			return null;
		} finally {
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(stream);
		}

		return filterCertFile.getAbsolutePath();

	}

}
