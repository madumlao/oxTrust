/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.ws.rs.scim2;

import static org.gluu.oxtrust.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_COUNT;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_FILTER;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static org.gluu.oxtrust.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxtrust.ldap.service.IGroupService;
import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.model.scim2.ListResponse;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.model.scim2.group.GroupResource;
import org.gluu.oxtrust.model.scim2.patch.PatchOperation;
import org.gluu.oxtrust.model.scim2.patch.PatchRequest;
import org.gluu.oxtrust.service.scim2.Scim2GroupService;
import org.gluu.oxtrust.service.scim2.Scim2PatchService;
import org.gluu.oxtrust.service.scim2.interceptor.RefAdjusted;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.persist.model.ListViewResponse;
import org.gluu.persist.model.SortOrder;
import org.joda.time.format.ISODateTimeFormat;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Implementation of /Groups endpoint. Methods here are intercepted and/or decorated.
 * Class org.gluu.oxtrust.service.scim2.interceptor.GroupWebServiceDecorator is used to apply pre-validations on data.
 * Filter org.gluu.oxtrust.filter.AuthorizationProcessingFilter secures invocations
 *
 * @author Rahat Ali Date: 05.08.2015
 * Updated by jgomer on 2017-10-18
 */
@Named("scim2GroupEndpoint")
@Path("/scim/v2/Groups")
@Api(value = "/v2/Groups", description = "SCIM 2.0 Group Endpoint (https://tools.ietf.org/html/rfc7644#section-3.2)",
        authorizations = {@Authorization(value = "Authorization", type = "uma")})
public class GroupWebService extends BaseScimWebService implements IGroupWebService {

    @Inject
    private UserWebService userWebService;

    @Inject
    private Scim2GroupService scim2GroupService;

    @Inject
    private IGroupService groupService;

    @Inject
    private Scim2PatchService scim2PatchService;

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "Create group", notes = "Create group (https://tools.ietf.org/html/rfc7644#section-3.3)", response = GroupResource.class)
    public Response createGroup(
            @ApiParam(value = "Group", required = true) GroupResource group,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. createGroup");
            scim2GroupService.createGroup(group, endpointUrl, userWebService.getEndpointUrl());
            String json=resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response=Response.created(new URI(group.getMeta().getLocation())).entity(json).build();
        }
        catch (Exception e){
            log.error("Failure at createGroup method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "Find group by id", notes = "Returns a group by id as path param (https://tools.ietf.org/html/rfc7644#section-3.4.2.1)",
            response = GroupResource.class)
    public Response getGroupById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getGroupById");

            GroupResource group=new GroupResource();
            GluuGroup gluuGroup=groupService.getGroupByInum(id);  //gluuGroup is not null (check associated decorator method)
            scim2GroupService.transferAttributesToGroupResource(gluuGroup, group, endpointUrl, userWebService.getEndpointUrl());

            String json=resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response=Response.ok(new URI(group.getMeta().getLocation())).entity(json).build();
        }
        catch (Exception e){
            log.error("Failure at getGroupById method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;
    }

    /**
     * This implementation differs from spec in the following aspects:
     * - Passing a null value for an attribute, does not modify the attribute in the destination, however passing an
     * empty array for a multivalued attribute does clear the attribute. Thus, to clear single-valued attribute, PATCH
     * operation should be used
     */
    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "Update group", notes = "Update group (https://tools.ietf.org/html/rfc7644#section-3.5.1)", response = GroupResource.class)
    public Response updateGroup(
            @ApiParam(value = "Group", required = true) GroupResource group,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateGroup");
            GroupResource updatedResource=scim2GroupService.updateGroup(id, group, endpointUrl, userWebService.getEndpointUrl());
            String json=resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response=Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at updateGroup method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @ApiOperation(value = "Delete group", notes = "Delete group (https://tools.ietf.org/html/rfc7644#section-3.6)")
    public Response deleteGroup(@PathParam("id") String id){

        Response response;
        try {
            log.debug("Executing web service method. deleteGroup");
            GluuGroup gr=groupService.getGroupByInum(id);  //group cannot be null (check associated decorator method)
            scim2GroupService.deleteGroup(gr);
            response=Response.noContent().build();
        }
        catch (Exception e){
            log.error("Failure at deleteGroup method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "Search groups", notes = "Returns a list of groups (https://tools.ietf.org/html/rfc7644#section-3.4.2.2)", response = ListResponse.class)
    public Response searchGroups(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. searchGroups");
            sortBy=translateSortByAttribute(GroupResource.class, sortBy);
            ListViewResponse<BaseScimResource> resources = scim2GroupService.searchGroups(filter, sortBy, SortOrder.getByValue(sortOrder),
                    startIndex, count, endpointUrl, userWebService.getEndpointUrl(), getMaxCount());

            String json = getListResponseSerialized(resources.getTotalResults(), startIndex, resources.getResult(), attrsList, excludedAttrsList, count==0);
            response=Response.ok(json).location(new URI(endpointUrl)).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at searchGroups method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "Search group POST /.search", notes = "Returns a list of groups (https://tools.ietf.org/html/rfc7644#section-3.4.3)", response = ListResponse.class)
    public Response searchGroupsPost(@ApiParam(value = "SearchRequest", required = true) SearchRequest searchRequest){

        log.debug("Executing web service method. searchGroupsPost");

        //Calling searchGroups here does not provoke that method's interceptor/decorator being called (only this one's)
        URI uri=null;
        Response response = searchGroups(searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "PATCH operation", notes = "https://tools.ietf.org/html/rfc7644#section-3.5.2", response = GroupResource.class)
    public Response patchGroup(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try{
            log.debug("Executing web service method. patchGroup");

            String usersUrl=userWebService.getEndpointUrl();
            GroupResource group=new GroupResource();
            GluuGroup gluuGroup=groupService.getGroupByInum(id);  //group is not null (check associated decorator method)

            //Fill group instance with all info from gluuGroup
            scim2GroupService.transferAttributesToGroupResource(gluuGroup, group, endpointUrl, usersUrl);

            //Apply patches one by one in sequence
            for (PatchOperation po : request.getOperations())
                group=(GroupResource) scim2PatchService.applyPatchOperation(group, po);

            //Throws exception if final representation does not pass overall validation
            log.debug("patchGroup. Revising final resource representation still passes validations");
            executeDefaultValidation(group);

            //Update timestamp
            String now=ISODateTimeFormat.dateTime().withZoneUTC().print(System.currentTimeMillis());
            group.getMeta().setLastModified(now);

            //Replaces the information found in gluuGroup with the contents of group
            scim2GroupService.replaceGroupInfo(gluuGroup, group, endpointUrl, usersUrl);

            String json=resourceSerializer.serialize(group, attrsList, excludedAttrsList);
            response=Response.ok(new URI(group.getMeta().getLocation())).entity(json).build();
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (SCIMException e){
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at patchGroup method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + GroupWebService.class.getAnnotation(Path.class).value();
    }

}
