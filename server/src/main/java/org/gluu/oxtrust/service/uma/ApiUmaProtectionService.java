/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxtrust.service.uma;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.exception.UmaProtectionException;
import org.gluu.oxtrust.service.OpenIdService;
import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.oxauth.client.ClientInfoClient;
import org.xdi.oxauth.client.ClientInfoResponse;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.util.Pair;

/**
 * Provides service to protect APIs Rest service endpoints with UMA scope.
 * 
 * @author Dmitry Ognyannikov
 */
@ApplicationScoped
@Named("apiUmaProtectionService")
@BindingUrls({"/api/v1"})
public class ApiUmaProtectionService extends BaseUmaProtectionService implements Serializable {
    
	private static final long serialVersionUID = 362749692619005003L;

    @Inject
    private Logger log;

	@Inject
    private AppConfiguration appConfiguration;

    @Inject
    private OpenIdService openIdService;

    @Override
    protected String getClientId() {
        return appConfiguration.getApiUmaClientId();
    }

    @Override
    protected String getClientKeyStorePassword() {
        return appConfiguration.getApiUmaClientKeyStorePassword();
    }

    @Override
    protected String getClientKeyStoreFile() {
        return appConfiguration.getApiUmaClientKeyStoreFile();
    }

    @Override
    protected String getClientKeyId() {
        return appConfiguration.getApiUmaClientKeyId();
    }

    @Override
    public String getUmaResourceId() {
        return appConfiguration.getApiUmaResourceId();
    }

    @Override
    public String getUmaScope() {
        return appConfiguration.getApiUmaScope();
    }

    @Override
    public boolean isEnabled() {
        return isEnabledUmaAuthentication();
    }

	@Override
	public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo) {
        Response authorizationResponse =  null;

        String authorization = headers.getHeaderString("Authorization");
        log.info("==== API Service call intercepted ====");
        log.info("Authorization header {} found", StringUtils.isEmpty(authorization) ? "not" : "");

        try {
            //Test mode may be removed in upcoming versions of Gluu Server...
            if (appConfiguration.isScimTestMode()) {
                log.info("API Test Mode is ACTIVE");
                authorizationResponse = processTestModeAuthorization(authorization);
            }
            else
            if (isEnabled()){
                log.info("API is protected by UMA");
                authorizationResponse = processUmaAuthorization(authorization, resourceInfo);
            }
            else{
                log.info("Please activate UMA or test mode to protect your API endpoints. Read the Gluu API docs to learn more");
                authorizationResponse= getErrorResponse(Response.Status.UNAUTHORIZED, "API not protected");
            }
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            authorizationResponse=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return authorizationResponse;
	}

	private Response processTestModeAuthorization(String token) throws Exception {
		Response response = null;

		if (StringUtils.isNotEmpty(token)) {
			token = token.replaceFirst("Bearer\\s+", "");
			log.debug("Validating token {}", token);

			String clientInfoEndpoint = openIdService.getOpenIdConfiguration().getClientInfoEndpoint();
			ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
			ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(token);

			if (clientInfoResponse.getErrorType() != null) {
				response = getErrorResponse(Status.UNAUTHORIZED, "Invalid token " + token);
				log.debug("Error validating access token: {}", clientInfoResponse.getErrorDescription());
			}
		} else {
			log.info("Request is missing authorization header");
			// see section 3.12 RFC 7644
			response = getErrorResponse(Status.INTERNAL_SERVER_ERROR, "No authorization header found");
		}
		return response;

	}

}
