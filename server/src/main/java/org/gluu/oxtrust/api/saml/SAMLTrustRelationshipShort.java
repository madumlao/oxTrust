/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxtrust.api.saml;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.gluu.oxtrust.model.GluuSAMLTrustRelationship;
import org.gluu.oxtrust.model.GluuValidationStatus;

/**
 * Data object for TrustRelationship short list of properties.
 * 
 * @author Dmitry Ognyannikov
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SAMLTrustRelationshipShort implements Serializable {
    private String inum;
    
    private String iname;
    
    private String displayName;
    
    private String description;
    
    private GluuValidationStatus validationStatus;
    
    private List<String> releasedAttributes;
    
    public SAMLTrustRelationshipShort() {}
    
    public SAMLTrustRelationshipShort(GluuSAMLTrustRelationship trustRelationship) {
        inum = trustRelationship.getInum();
        iname = trustRelationship.getIname();
        displayName = trustRelationship.getDisplayName();
        description = trustRelationship.getDescription();
        validationStatus = trustRelationship.getValidationStatus();
        releasedAttributes = trustRelationship.getReleasedAttributes();
    }

    /**
     * @return the inum
     */
    public String getInum() {
        return inum;
    }

    /**
     * @param inum the inum to set
     */
    public void setInum(String inum) {
        this.inum = inum;
    }

    /**
     * @return the iname
     */
    public String getIname() {
        return iname;
    }

    /**
     * @param iname the iname to set
     */
    public void setIname(String iname) {
        this.iname = iname;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the validationStatus
     */
    public GluuValidationStatus getValidationStatus() {
        return validationStatus;
    }

    /**
     * @param validationStatus the validationStatus to set
     */
    public void setValidationStatus(GluuValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    /**
     * @return the releasedAttributes
     */
    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    /**
     * @param releasedAttributes the releasedAttributes to set
     */
    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }
}
