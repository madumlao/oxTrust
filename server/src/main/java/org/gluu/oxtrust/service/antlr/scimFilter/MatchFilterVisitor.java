/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.service.antlr.scimFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.service.antlr.scimFilter.antlr4.ScimFilterBaseVisitor;
import org.gluu.oxtrust.service.antlr.scimFilter.antlr4.ScimFilterParser;
import org.gluu.oxtrust.service.antlr.scimFilter.enums.CompValueType;
import org.gluu.oxtrust.service.antlr.scimFilter.enums.ScimOperator;
import org.gluu.oxtrust.service.antlr.scimFilter.util.FilterUtil;
import org.gluu.oxtrust.service.antlr.scimFilter.util.SimpleExpression;
import org.gluu.oxtrust.service.scim2.ExtensionService;

import javax.inject.Inject;
import java.util.Map;

/**
 * Created by jgomer on 2017-12-10.
 */
public class MatchFilterVisitor extends ScimFilterBaseVisitor<Boolean> {

    private Logger log = LogManager.getLogger(getClass());
    private Map<String, Object> item;
    private String parentAttribute;
    private Class<? extends BaseScimResource> resourceClass;

    public MatchFilterVisitor(Map<String, Object> item, String parentAttribute, Class<? extends BaseScimResource> resourceClass){
        this.item=item;
        this.resourceClass = resourceClass;
        this.parentAttribute = parentAttribute;
    }

    @Override
    public Boolean visitNegatedFilter(ScimFilterParser.NegatedFilterContext ctx) {
        Boolean val=visit(ctx.filter());
        log.trace("visitNegatedFilter. childs: {}, text: {}", ctx.getChildCount(), ctx.getText());
        return ctx.getText().startsWith("not(") ? !val : val;
    }

    @Override
    public Boolean visitOrFilter(ScimFilterParser.OrFilterContext ctx) {
        log.trace("visitOrFilter. childs: {}, text: {}", ctx.getChildCount(), ctx.getText());
        return visit(ctx.getChild(0)) || visit(ctx.getChild(2));
    }

    @Override
    public Boolean visitAndFilter(ScimFilterParser.AndFilterContext ctx) {
        log.trace("visitAndFilter. childs: {}, text: {}", ctx.getChildCount(), ctx.getText());
        return visit(ctx.getChild(0)) && visit(ctx.getChild(2));
    }

    @Override
    public Boolean visitAttrexp(ScimFilterParser.AttrexpContext ctx) {
        log.trace("visitAttrexp. childs: {}, text: {}", ctx.getChildCount(), ctx.getText());

        String path=ctx.attrpath().getText();
        ScimFilterParser.CompvalueContext compValueCtx =ctx.compvalue();
        boolean isPrRule= compValueCtx==null && ctx.getChild(1).getText().equals("pr");

        ScimOperator operator;
        CompValueType valueType;
        String value;

        if (isPrRule){
            operator=ScimOperator.NOT_EQUAL;
            valueType=CompValueType.NULL;
            value=null;
        }
        else{
            operator=ScimOperator.getByValue(ctx.compareop().getText());
            valueType= FilterUtil.getCompValueType(compValueCtx);
            value=compValueCtx.getText();

            if (CompValueType.STRING.equals(valueType)) //drop double quotes
                value=value.substring(1, value.length()-1);
        }

        SimpleExpression expr = new SimpleExpression(path, operator, valueType, value);
        expr.setParentAttribute(parentAttribute);
        expr.setResourceClass(resourceClass);
        return expr.evaluate(item);

    }

}
