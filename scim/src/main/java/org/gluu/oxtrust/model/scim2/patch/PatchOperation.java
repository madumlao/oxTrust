/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.model.scim2.patch;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents a patch operation as per section 4 of RFC 6902. See also {@link PatchRequest PatchRequest}.
 */
/*
 * Created by jgomer on 2017-10-28.
 */
public class PatchOperation {

    @JsonProperty("op")
    private String operation;

    private String path;

    private Object value;

    @JsonIgnore
    private PatchOperationType type;

    public void setValue(Object value){
        this.value=value;
    }

    public Object getValue(){
        return value;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;

        if (operation!=null)
            type=PatchOperationType.valueOf(operation.toUpperCase());
    }

    public String getPath() {
        return path;
    }

    public PatchOperationType getType() {
        return type;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
