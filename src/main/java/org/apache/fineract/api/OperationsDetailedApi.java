package org.apache.fineract.api;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.operations.*;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.utils.DateUtil;
import org.keycloak.KeycloakPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.*;

import static org.apache.fineract.core.service.OperatorUtils.dateFormat;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Operations Detailed API")
@SecurityRequirement(name = "api")
public class OperationsDetailedApi {

    private static final String PARSE_DATE_FAILURE_MESSAGE = "failed to parse dates {} / {}";
    private static final String STARTED_AT_STRING = "startedAt";
    private static final String UTF = "UTF-8";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransactionRequestRepository transactionRequestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DateUtil dateUtil;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AMSConfig amsConfig;

    private final String transfersResourceNameForPermissions = "TRANSFER";
    private final String transactionRequestsResourceNameForPermissions = "TRANSACTION_REQUEST";

    @GetMapping("/ams/sources")
    public List<AMSConfig.AmsSource> getAmsSourcesList() {
        return amsConfig.getAmsSourcesList();
    }

    @GetMapping("/transfers")
    public Page<TransferResponse> transfers(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page, @RequestParam(value = "size", required = false, defaultValue = "20") Integer size, @RequestParam(value = "payerPartyId", required = false) String payerPartyId, @RequestParam(value = "payerDfspId", required = false) String payerDfspId, @RequestParam(value = "payeePartyId", required = false) String payeePartyId, @RequestParam(value = "payeeDfspId", required = false) String payeeDfspId, @RequestParam(value = "transactionId", required = false) String transactionId, @RequestParam(value = "status", required = false) String status, @RequestParam(value = "amount", required = false) BigDecimal amount, @RequestParam(value = "currency", required = false) String currency, @RequestParam(value = "startFrom", required = false) String startFrom, @RequestParam(value = "startTo", required = false) String startTo, @RequestParam(value = "direction", required = false) String direction, @RequestParam(value = "sortedBy", required = false) String sortedBy, @RequestParam(value = "partyId", required = false) String partyId, @RequestParam(value = "partyIdType", required = false) String partyIdType, @RequestParam(value = "clientCorrelationId", required = false) String clientCorrelationId, @RequestParam(value = "sortedOrder", required = false, defaultValue = "DESC") String sortedOrder) {
        ThreadLocalContextUtil.getCurrentUser().validateHasReadPermission(this.transfersResourceNameForPermissions);
        List<Specifications<Transfer>> specs = getSearchSpecifications(status, amount, currency, direction, partyId, partyIdType, clientCorrelationId);
        specs.addAll(getSearchSpecification(payerPartyId, payerDfspId, payeeDfspId, payeePartyId, transactionId));

        specs.addAll(getDateSearchSpecs(startFrom, startTo));

        PageRequest pager;
        if (sortedBy == null || STARTED_AT_STRING.equals(sortedBy)) {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), STARTED_AT_STRING));
        } else {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), sortedBy));
        }

        Page<Transfer> transferPage = getTransfers(specs, pager);

        List<TransferResponse> transferResponseList = new ArrayList<>();
        for (Transfer transfer : transferPage.getContent()) {
            TransferResponse transferResponse = null;
            try {
                String json = transfer.getErrorInformation();
                transfer.setErrorInformation(null);
                transferResponse = objectMapper.readValue(objectMapper.writeValueAsString(transfer), TransferResponse.class);
                String businessShortCode = transfer.getAmsBusinessShortCode();
                if (!StringUtils.isNullOrEmpty(businessShortCode)) {
                    transferResponse.setPayerDfspId(businessShortCode);
                }
                transferResponse.parseErrorInformation(json, objectMapper);
                transferResponseList.add(transferResponse);
            } catch (Exception e) {
                logger.error("Error parsing errorInformation into DTO: {}", e.getMessage());
                e.printStackTrace();
                if (transferResponse != null) {
                    transferResponseList.add(transferResponse);
                }
            }
        }

        return new PageImpl<>(transferResponseList, transferPage.getPageable(), transferPage.getTotalPages());

    }

    /**
     * Get the list of transfers based on the specs
     * @param specs list of specifications
     * @param pager page request
     * @return page of transfers
     */
    private Page<Transfer> getTransfers(List<Specifications<Transfer>> specs, PageRequest pager) {
        Page<Transfer> transferPage;
        if (specs.size() > 0) {
            Specifications<Transfer> compiledSpecs = specs.get(0);
            for (int i = 1; i < specs.size(); i++) {
                compiledSpecs = compiledSpecs.and(specs.get(i));
            }
            transferPage = transferRepository.findAll(compiledSpecs, pager);
        } else {
            transferPage = transferRepository.findAll(pager);
        }
        return transferPage;
    }

    private Collection<Specifications<Transfer>> getDateSearchSpecs(String startFrom, String startTo) {
        List<Specifications<Transfer>> specs = new ArrayList<>();
        if (startFrom != null) {
            startFrom = dateUtil.getUTCFormat(startFrom);
        }
        if (startTo != null) {
            startTo = dateUtil.getUTCFormat(startTo);
        }
        try {
            if (startFrom != null && startTo != null) {
                specs.add(TransferSpecs.between(Transfer_.startedAt, dateFormat().parse(startFrom), dateFormat().parse(startTo)));
            } else if (startFrom != null) {
                specs.add(TransferSpecs.later(Transfer_.startedAt, dateFormat().parse(startFrom)));
            } else if (startTo != null) {
                specs.add(TransferSpecs.earlier(Transfer_.startedAt, dateFormat().parse(startTo)));
            }
        } catch (Exception e) {
            logger.warn(PARSE_DATE_FAILURE_MESSAGE, startFrom, startTo);
        }
        return specs;
    }

    private List<Specifications<Transfer>> getSearchSpecification(String payerPartyId, String payerDfspId, String payeeDfspId, String payeePartyId, String transactionId) {
        List<Specifications<Transfer>> specs = new ArrayList<>();
        if (payerPartyId != null) {
            specs.add(getPayerPartyIdSearchSpec(payerPartyId));
        }
        if (payeePartyId != null) {
            specs.add(getPayeePartyIdSearchSpec(payeePartyId));
        }
        if (payeeDfspId != null) {
            specs.add(TransferSpecs.match(Transfer_.payeeDfspId, payeeDfspId));
        }
        if (payerDfspId != null) {
            specs.add(TransferSpecs.match(Transfer_.amsBusinessShortCode, payerDfspId));
        }
        if (transactionId != null) {
            specs.add(TransferSpecs.match(Transfer_.transactionId, transactionId));
        }
        return specs;
    }

    private List<Specifications<Transfer>> getSearchSpecifications(String status, BigDecimal amount, String currency, String direction, String partyId, String partyIdType, String clientCorrelationId) {
        List<Specifications<Transfer>> specs = new ArrayList<>();
        if (clientCorrelationId != null) {
            specs.add(TransferSpecs.like(Transfer_.clientCorrelationId, clientCorrelationId));
        }
        if (status != null && parseStatus(status) != null) {
            specs.add(TransferSpecs.match(Transfer_.status, parseStatus(status)));
        }
        if (amount != null) {
            specs.add(TransferSpecs.match(Transfer_.amount, amount));
        }
        if (currency != null) {
            specs.add(TransferSpecs.match(Transfer_.currency, currency));
        }
        if (direction != null) {
            specs.add(TransferSpecs.match(Transfer_.direction, direction));
        }
        if (partyIdType != null) {
            specs.add(TransferSpecs.multiMatch(Transfer_.payeePartyIdType, Transfer_.payerPartyIdType, partyIdType));
        }
        if (partyId != null) {
            specs.add(getPartyIdSearchSpec(partyId));
        }
        return specs;
    }

    private Specifications<Transfer> getPartyIdSearchSpec(String partyId) {
        if (partyId.contains("%2B")) {
            try {
                partyId = URLDecoder.decode(partyId, UTF);
                logger.info("Decoded PartyId: {}", partyId);
            } catch (UnsupportedEncodingException e) {
                logger.info(e.getLocalizedMessage());
            }
        }
        return TransferSpecs.multiMatch(Transfer_.payerPartyId, Transfer_.payeePartyId, partyId);
    }

    private Specifications<Transfer> getPayerPartyIdSearchSpec(String payerPartyId) {
        if (payerPartyId.contains("%2B")) {
            try {
                payerPartyId = URLDecoder.decode(payerPartyId, UTF);
                logger.info("Decoded payerPartyId: {}", payerPartyId);
            } catch (UnsupportedEncodingException e) {
                logger.info(e.getLocalizedMessage());
            }
        }
        return TransferSpecs.match(Transfer_.payerPartyId, payerPartyId);
    }

    private Specifications<Transfer> getPayeePartyIdSearchSpec(String payeePartyId) {
        if (payeePartyId.contains("%2B")) {
            try {
                payeePartyId = URLDecoder.decode(payeePartyId, UTF);
                logger.info("Decoded payeePartyId: {}", payeePartyId);
            } catch (UnsupportedEncodingException e) {
                logger.info(e.getLocalizedMessage());
            }
        }
        return TransferSpecs.match(Transfer_.payeePartyId, payeePartyId);
    }

    @GetMapping("/transactionRequests")
    public Page<TransactionRequest> transactionRequests(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page, @RequestParam(value = "size", required = false, defaultValue = "20") Integer size, @RequestParam(value = "payerPartyId", required = false) String payerPartyId, @RequestParam(value = "payeePartyId", required = false) String payeePartyId, @RequestParam(value = "payeePartyIdType", required = false) String payeePartyIdType, @RequestParam(value = "payeeDfspId", required = false) String payeeDfspId, @RequestParam(value = "payerDfspId", required = false) String payerDfspId, @RequestParam(value = "transactionId", required = false) String transactionId, @RequestParam(value = "state", required = false) String state, @RequestParam(value = "amount", required = false) BigDecimal amount, @RequestParam(value = "currency", required = false) String currency, @RequestParam(value = "startFrom", required = false) String startFrom, @RequestParam(value = "startTo", required = false) String startTo, @RequestParam(value = "direction", required = false) String direction, @RequestParam(value = "clientCorrelationId", required = false) String clientCorrelationId,@RequestParam(value = "externalId", required = false) String externalId, @RequestParam(value = "sortedBy", required = false) String sortedBy, @RequestParam(value = "sortedOrder", required = false, defaultValue = "DESC") String sortedOrder) {
        ThreadLocalContextUtil.getCurrentUser().validateHasReadPermission(this.transactionRequestsResourceNameForPermissions);
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (payerPartyId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payerPartyId, payerPartyId));
        }
        if (payeeDfspId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeeDfspId, payeeDfspId));
        }
        if (payerDfspId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.amsBusinessShortCode, payerDfspId));
        }
        if (transactionId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.transactionId, transactionId));
        }
        if (state != null && parseState(state) != null) {
            specs.add(TransactionRequestSpecs.match(TransactionRequest_.state, parseState(state)));
        }
        if (amount != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.amount, amount));
        }
        if (clientCorrelationId != null) {
            specs.add(TransactionRequestSpecs.match(TransactionRequest_.clientCorrelationId, clientCorrelationId));
        }
        if (externalId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.externalId, externalId));
        }
        if (direction != null) {
            specs.add(TransactionRequestSpecs.match(TransactionRequest_.direction, direction));
        }
        List<Specifications<TransactionRequest>> dateSpecs = checkDates(startFrom, startTo);
        if (!dateSpecs.isEmpty()) specs.addAll(dateSpecs);

        PageRequest pageRequest = getPager(sortedBy, page, size, sortedOrder);

        return transactionRequestFilter(pageRequest, specs, currency, payeePartyId, payeePartyIdType);
    }

    private List<Specifications<TransactionRequest>> checkDates(String startFrom, String startTo) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        try {
            if (startFrom != null) {
                startFrom = dateUtil.getUTCFormat(startFrom);
            }
            if (startTo != null) {
                startTo = dateUtil.getUTCFormat(startTo);
            }
            if (startFrom != null && startTo != null) {
                specs.add(TransactionRequestSpecs.between(TransactionRequest_.startedAt, dateFormat().parse(startFrom), dateFormat().parse(startTo)));
            } else if (startFrom != null) {
                specs.add(TransactionRequestSpecs.later(TransactionRequest_.startedAt, dateFormat().parse(startFrom)));
            } else if (startTo != null) {
                specs.add(TransactionRequestSpecs.earlier(TransactionRequest_.startedAt, dateFormat().parse(startTo)));
            }
        } catch (Exception e) {
            logger.warn(PARSE_DATE_FAILURE_MESSAGE, startFrom, startTo);
        }
        return specs;
    }

    private PageRequest getPager(String sortedBy, Integer page, Integer size, String sortedOrder) {
        PageRequest pager;
        if (sortedBy == null || STARTED_AT_STRING .equals(sortedBy)) {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.valueOf(sortedOrder), STARTED_AT_STRING));
        } else {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.valueOf(sortedOrder), sortedBy));
        }
        return pager;
    }

    public Page<TransactionRequest> transactionRequestFilter(PageRequest pager, List<Specifications<TransactionRequest>> specs, String currency, String payeePartyId, String payeePartyIdType) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Check if the user is authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            specs.addAll(checkAssignments(authentication, payeePartyId, payeePartyIdType, currency));
        } else {
            logger.info("authenticated user not found");
            return new PageImpl<>(Collections.emptyList(), pager, 0);
        }

        return transactionRequestsResponse(specs, pager);
    }

    private List<Specifications<TransactionRequest>> checkAssignments(Authentication authentication, String payeePartyId, String payeePartyIdType, String currency) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        // Get the authenticated user by username
        KeycloakPrincipal<?> principal = (KeycloakPrincipal<?>)authentication.getPrincipal();
        AppUser currentUser = appUserRepository.findAppUserByName(principal.getKeycloakSecurityContext().getToken().getPreferredUsername());
        // filter transactions by dukas assigned to the user
        specs.addAll(getDukasSpecs(currentUser, payeePartyId));
        // filter transactions by currencies assigned to the user
        specs.addAll(getCurrenciesSpecs(currentUser, currency));
        // filter transactions by PayeePartyIdTypes assigned to the user
        specs.addAll(getPayeePartyIdTypeSpecs(currentUser, payeePartyIdType));
        return specs;
    }

    private Collection<? extends Specifications<TransactionRequest>> getPayeePartyIdTypeSpecs(AppUser currentUser, String payeePartyIdType) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (currentUser.getPayeePartyIdTypesList().isEmpty()) {
            // user not allowed to see any PayeePartyIdTypes data, return empty page of transactions
            logger.info("user not allowed to see any PayeePartyIdTypes data");
            return new ArrayList<>();
        } else if (currentUser.getPayeePartyIdTypesList().equals(Collections.singletonList("*"))) {
            // user is allowed to see data from all payeePartyIdTypes. Check if they wanna filter for a specific payee, otherwise don't add to spec
            if (payeePartyIdType != null) {
                specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeePartyIdType, payeePartyIdType));
            }
        } else {
            List<Specifications<TransactionRequest>> partyIdTypeSpecs = checkUserPayeePartyIdTypesAssigned(currentUser, payeePartyIdType);
            if (!partyIdTypeSpecs.isEmpty())
                specs.addAll(partyIdTypeSpecs);
        }
        return specs;
    }

    private Collection<? extends Specifications<TransactionRequest>> getCurrenciesSpecs(AppUser currentUser, String currency) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (currentUser.getCurrenciesList().isEmpty()) {
            // user not allowed to see any currency data, return empty page of transactions
            logger.info("user not allowed to see any currency data");
            return new ArrayList<>();
        } else if (currentUser.getCurrenciesList().equals(Collections.singletonList("*"))) {
            // user is allowed to see data from all currencies. Check if they wanna filter for a specific currency, otherwise don't add to spec
            if (currency != null) {
                specs.add(TransactionRequestSpecs.like(TransactionRequest_.currency, currency));
            }
        } else {
            List<Specifications<TransactionRequest>> currencySpecs = checkUserCurrenciesAssigned(currentUser, currency);
            if (!currencySpecs.isEmpty())
                specs.addAll(currencySpecs);
        }
        return specs;
    }

    private Collection<? extends Specifications<TransactionRequest>> getDukasSpecs(AppUser currentUser, String payeePartyId) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (currentUser.getPayeePartyIdsList().isEmpty()) {
            // user not allowed to see any duka data, return empty page of transactions
            logger.info("user not allowed to see any duka data");
            return new ArrayList<>();
        } else if (currentUser.getPayeePartyIdsList().equals(Collections.singletonList("*"))) {
            // user is allowed to see data from all dukas. Check if they wanna filter for a specific payee, otherwise don't add to spec
            if (payeePartyId != null) {
                specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeePartyId, payeePartyId));
            }
        } else {
            List<Specifications<TransactionRequest>> dukaSpecs = checkUserDukasAssigned(currentUser, payeePartyId);
            if (!dukaSpecs.isEmpty())
                specs.addAll(dukaSpecs);
        }
        return specs;
    }

    private Page<TransactionRequest> transactionRequestsResponse(List<Specifications<TransactionRequest>> specs, PageRequest pager) {
        if (!specs.isEmpty()) {
            Specifications<TransactionRequest> compiledSpecs = specs.get(0);
            for (int i = 1; i < specs.size(); i++) {
                compiledSpecs = compiledSpecs.and(specs.get(i));
            }
            return transactionRequestRepository.findAll(compiledSpecs, pager);
        } else {
            return transactionRequestRepository.findAll(pager);
        }
    }

    public List<Specifications<TransactionRequest>> checkUserCurrenciesAssigned(AppUser currentUser, String currency) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();


        // user is assigned currency in the list
        if (currency != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.currency, currency));
        } else {
            specs.add(TransactionRequestSpecs.in(TransactionRequest_.currency, currentUser.getCurrenciesList()));
        }

        return specs;
    }

    public List<Specifications<TransactionRequest>> checkUserDukasAssigned(AppUser currentUser, String payeePartyId) {

        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        // user is assigned dukas in the list
        if (payeePartyId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeePartyId, payeePartyId));
        } else {
            specs.add(TransactionRequestSpecs.in(TransactionRequest_.payeePartyId, currentUser.getPayeePartyIdsList()));
        }
        return specs;
    }

    public List<Specifications<TransactionRequest>> checkUserPayeePartyIdTypesAssigned(AppUser currentUser, String payeePartyIdType) {
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();

        // user is assigned payeePartyIdTypes in the list
        if (payeePartyIdType != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeePartyIdType, payeePartyIdType));
        } else {
            specs.add(TransactionRequestSpecs.in(TransactionRequest_.payeePartyIdType, currentUser.getPayeePartyIdTypesList()));
        }


        return specs;
    }

    /*
     * Parses the [TransferStatus] enum from transactionStatus string
     */
    private TransferStatus parseStatus(@RequestParam(value = "transactionStatus", required = false) String transactionStatus) {
        try {
            return transactionStatus == null ? null : TransferStatus.valueOf(transactionStatus);
        } catch (Exception e) {
            logger.warn("failed to parse transaction status {}, ignoring it", transactionStatus);
            return null;
        }
    }

    /*
     * Parses the [TransactionRequestState] enum from transactionState string
     */
    private TransactionRequestState parseState(String state) {
        try {
            return state == null ? null : TransactionRequestState.valueOf(state);
        } catch (Exception e) {
            logger.warn("failed to parse TransactionRequestState {}, ignoring it", state);
            return null;
        }
    }
}
