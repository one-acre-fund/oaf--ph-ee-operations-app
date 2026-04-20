/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.organisation.permission;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable representation of permissions
 */
public class PermissionData {

    @SuppressWarnings("unused")
    @JsonProperty("grouping")
    private String module;
    @SuppressWarnings("unused")
    private String code;
    @SuppressWarnings("unused")
    private String entityName;
    @SuppressWarnings("unused")
    private String actionName;
    @SuppressWarnings("unused")
    private Boolean selected;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public PermissionData() {}

    public static PermissionData from(final String permissionCode, final boolean isSelected) {
        return new PermissionData(null, permissionCode, null, null, isSelected);
    }

    public static PermissionData instance(final String module, final String code, final String entityName, final String actionName,
            final Boolean selected) {
        return new PermissionData(module, code, entityName, actionName, selected);
    }

    public PermissionData(final String module, final String code, final String entityName, final String actionName,
            final Boolean selected) {
        this.module = module;
        this.code = code;
        this.entityName = entityName;
        this.actionName = actionName;
        this.selected = selected;
    }
}
