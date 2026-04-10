/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.organisation.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Model to hold the user details, and their authorization data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUserDto {
    private String email;

    private String username;

    private String firstname;

    private String lastname;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private boolean enabled;

    private boolean firstTimeLoginRemaining;

    private boolean deleted;

    private Date lastTimePasswordUpdated;

    private boolean passwordNeverExpires;

    private List<String> payeePartyIds;

    private List<String> currencies;

    private List<String> payeePartyIdTypes;

    private Set<String> permissions;

    private Set<String> roles;

    public AppUserDto(AppUser user, Set<String> uniquePermissions, Set<String> roles) {
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.firstname = user.getFirstname();
        this.lastname = user.getLastname();
        this.accountNonExpired = user.isAccountNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.enabled = user.isEnabled();
        this.firstTimeLoginRemaining = user.isFirstTimeLoginRemaining();
        this.deleted = user.isDeleted();
        this.lastTimePasswordUpdated = user.getLastTimePasswordUpdated();
        this.passwordNeverExpires = user.isPasswordNeverExpires();
        this.payeePartyIds = user.getPayeePartyIdsList();
        this.currencies = user.getCurrenciesList();
        this.payeePartyIdTypes = user.getPayeePartyIdTypesList();
        this.permissions = uniquePermissions;
        this.roles = roles;
    }
}
