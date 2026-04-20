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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Permission findOneByCode(String code);
    
    @Query("SELECT DISTINCT p.entityName FROM Permission p")
    List<String> findDistinctEntityName();

    @Query("SELECT DISTINCT p.actionName FROM Permission p")
    List<String> findDistinctActionNames();
    
    @Query("SELECT new org.apache.fineract.organisation.permission.PermissionData(p.module, p.code, p.entityName, p.actionName, " +
           "CASE WHEN EXISTS (SELECT 1 FROM Role r JOIN r.permissions rp WHERE r.id = :roleId AND rp.id = p.id) THEN true ELSE false END) " +
           "FROM Permission p " +
           "ORDER BY p.module, p.code")
    List<PermissionData> findAllPermissionsWithRoleSelection(@Param("roleId") Long roleId);
}
