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
package org.apache.fineract.organisation.staff;

import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.config.CustomAuditingEntityListener;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.document.Image;
import org.apache.fineract.organisation.office.Office;
import org.joda.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.EntityListeners;

import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "m_staff")
@EntityListeners(CustomAuditingEntityListener.class)
public class Staff extends AbstractPersistableCustom<Long> {

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "lastname", length = 50)
    private String lastname;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "mobile_no", length = 50, nullable = false, unique = true)
    private String mobileNo;

    @Column(name = "external_id", length = 100, nullable = true, unique = true)
    private String externalId;

	@Column(name = "email_address", length = 50, unique = true)
    private String emailAddress;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(name = "is_loan_officer", nullable = false)
    private boolean loanOfficer;

    @Column(name = "organisational_role_enum", nullable = true)
    private Integer organisationalRoleType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "joining_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date joiningDate;

    @ManyToOne
    @JoinColumn(name = "organisational_role_parent_staff_id", nullable = true)
    private Staff organisationalRoleParentStaff;

    @OneToOne(optional = true)
    @JoinColumn(name = "image_id", nullable = true)
    private Image image;

    protected Staff() {
        //
    }

    private Staff(final Office staffOffice, final String firstname, final String lastname, final String externalId, final String mobileNo,
            final boolean isLoanOfficer, final Boolean isActive, final LocalDate joiningDate) {
        this.office = staffOffice;
        this.firstname = StringUtils.defaultIfEmpty(firstname, null);
        this.lastname = StringUtils.defaultIfEmpty(lastname, null);
        this.externalId = StringUtils.defaultIfEmpty(externalId, null);
        this.mobileNo = StringUtils.defaultIfEmpty(mobileNo, null);
        this.loanOfficer = isLoanOfficer;
        this.active = (isActive == null) ? true : isActive;
        deriveDisplayName(firstname);
        if (joiningDate != null) {
            this.joiningDate = joiningDate.toDateTimeAtStartOfDay().toDate();
        }
    }

    public EnumOptionData organisationalRoleData() {
        EnumOptionData organisationalRole = null;
        if (this.organisationalRoleType != null) {
            organisationalRole = StaffEnumerations.organisationalRole(this.organisationalRoleType);
        }
        return organisationalRole;
    }

    public void changeOffice(final Office newOffice) {
        this.office = newOffice;
    }


    public boolean isNotLoanOfficer() {
        return !isLoanOfficer();
    }

    public boolean isLoanOfficer() {
        return this.loanOfficer;
    }

    public boolean isNotActive() {
        return !isActive();
    }

    public boolean isActive() {
        return this.active;
    }

    private void deriveDisplayName(final String firstname) {
        if (!StringUtils.isBlank(firstname)) {
            this.displayName = this.lastname + ", " + this.firstname;
        } else {
            this.displayName = this.lastname;
        }
    }

    public boolean identifiedBy(final Staff staff) {
        return getId().equals(staff.getId());
    }

	public String emailAddress() {
        return emailAddress;
    }

    public Long officeId() {
        return this.office.getId();
    }

    public String displayName() {
        return this.displayName;
    }

    public String mobileNo() {
        return this.mobileNo;
    }

    public Office office() {
        return this.office;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return this.image;
    }
}