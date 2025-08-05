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
package org.apache.fineract.config.security.service;


import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.apache.fineract.config.CacheConfig.CACHE_USER_BY_NAME;

/**
 * Used in securityContext.xml as implementation of spring security's {@link UserDetailsService}.
 */
@Service("userDetailsService")
public class TenantAwareJpaPlatformUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    public TenantAwareJpaPlatformUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Cacheable(cacheNames = CACHE_USER_BY_NAME)
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException, DataAccessException {

        final AppUser appUser = this.appUserRepository.findAppUserByName(username);

        if (appUser == null) {
            throw new UsernameNotFoundException(username + ": not found");
        }

        return appUser;
    }
}
