package org.apache.fineract.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.fineract.data.ErrorResponse;
import org.apache.fineract.exception.WriteToCsvException;
import org.apache.fineract.operations.*;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.utils.CsvUtility;
import org.apache.fineract.utils.DateUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.*;

import static org.apache.fineract.core.service.OperatorUtils.dateFormat;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "auth")
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

    @GetMapping("/ams/sources")
    public List<AMSConfig.AmsSource> getAmsSourcesList() {
        return amsConfig.getAmsSourcesList();
    }

    @GetMapping("/transfers")
    public Page<TransferResponse> transfers(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page, @RequestParam(value = "size", required = false, defaultValue = "1") Integer size, @RequestParam(value = "payerPartyId", required = false) String payerPartyId, @RequestParam(value = "payerDfspId", required = false) String payerDfspId, @RequestParam(value = "payeePartyId", required = false) String payeePartyId, @RequestParam(value = "payeeDfspId", required = false) String payeeDfspId, @RequestParam(value = "transactionId", required = false) String transactionId, @RequestParam(value = "status", required = false) String status, @RequestParam(value = "amount", required = false) BigDecimal amount, @RequestParam(value = "currency", required = false) String currency, @RequestParam(value = "startFrom", required = false) String startFrom, @RequestParam(value = "startTo", required = false) String startTo, @RequestParam(value = "direction", required = false) String direction, @RequestParam(value = "sortedBy", required = false) String sortedBy, @RequestParam(value = "partyId", required = false) String partyId, @RequestParam(value = "partyIdType", required = false) String partyIdType, @RequestParam(value = "clientCorrelationId", required = false) String clientCorrelationId, @RequestParam(value = "sortedOrder", required = false, defaultValue = "DESC") String sortedOrder) {
        List<Specifications<Transfer>> specs = getSearchSpecifications(status, amount, currency, direction, partyId, partyIdType, clientCorrelationId);
        specs.addAll(getSearchSpecification(payerPartyId, payerDfspId, payeeDfspId, payeePartyId, transactionId));

        specs.addAll(getDateSearchSpecs(startFrom, startTo));

        PageRequest pager;
        if (sortedBy == null || STARTED_AT_STRING.equals(sortedBy)) {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), STARTED_AT_STRING));
        } else {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), sortedBy));
        }

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

        List<TransferResponse> transferResponseList = new ArrayList<>();
        int i = 0;
        for (Transfer transfer : transferPage.getContent()) {
            TransferResponse transferResponse = null;
            try {
                String json = transfer.getErrorInformation();
                transfer.setErrorInformation(null);
                transferResponse = objectMapper.readValue(objectMapper.writeValueAsString(transfer), TransferResponse.class);
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
            specs.add(TransferSpecs.match(Transfer_.payerDfspId, payerDfspId));
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
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (payerPartyId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payerPartyId, payerPartyId));
        }
        if (payeeDfspId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payeeDfspId, payeeDfspId));
        }
        if (payerDfspId != null) {
            specs.add(TransactionRequestSpecs.like(TransactionRequest_.payerDfspId, payerDfspId));
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
        AppUser currentUser = appUserRepository.findAppUserByName(authentication.getName());
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

    /**
     * Filter the [TransactionRequests] based on multiple type of ids
     *
     * @param response    instance of HttpServletResponse
     * @param page        the count/number of page which we want to fetch
     * @param size        the size of the single page defaults to [10000]
     * @param sortedOrder the order of sorting [ASC] or [DESC], defaults to [DESC]
     * @param startFrom   use for filtering records after this date, format: "yyyy-MM-dd HH:mm:ss"
     * @param startTo     use for filtering records before this date
     * @param state       filter based on state of the transaction
     */
    @PostMapping("/transactionRequests")
    public Map<String, String> filterTransactionRequests(HttpServletResponse response, @RequestParam(value = "command", required = false, defaultValue = "export") String command, @RequestParam(value = "page", required = false, defaultValue = "0") Integer page, @RequestParam(value = "size", required = false, defaultValue = "10000") Integer size, @RequestParam(value = "sortedOrder", required = false, defaultValue = "DESC") String sortedOrder, @RequestParam(value = "startFrom", required = false) String startFrom, @RequestParam(value = "startTo", required = false) String startTo, @RequestParam(value = "state", required = false) String state, @RequestBody Map<String, List<String>> body) {

        if (!command.equalsIgnoreCase("export")) {
            return new ErrorResponse.Builder().setErrorCode("" + HttpServletResponse.SC_NOT_FOUND).setErrorDescription(command + " not supported").setDeveloperMessage("Possible supported command is " + command).build();
        }

        List<String> filterByList = new ArrayList<>(body.keySet());

        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        if (state != null && parseState(state) != null) {
            specs.add(TransactionRequestSpecs.match(TransactionRequest_.state, parseState(state)));
            logger.info("State filter added");
        }
        if (startFrom != null) {
            startFrom = dateUtil.getUTCFormat(startFrom);
        }
        if (startTo != null) {
            startTo = dateUtil.getUTCFormat(startTo);
        }
        try {
            specs.add(getDateSpecification(startTo, startFrom));
            logger.info("Date filter parsed successful");
        } catch (Exception e) {
            logger.warn(PARSE_DATE_FAILURE_MESSAGE, startFrom, startTo);
        }

        Specifications<TransactionRequest> spec = null;
        for (String filterBy : filterByList) {
            List<String> ids = body.get(filterBy);
            if (!ids.isEmpty()) {
                Filter filter;
                try {
                    filter = parseFilter(filterBy);
                    logger.info("Filter parsed successfully {}", filter.name());
                } catch (Exception e) {
                    logger.info("Unable to parse filter {} skipping", filterBy);
                    continue;
                }
                spec = getFilterSpecs(filter, ids);
                specs.add(spec);
            }
        }
        PageRequest pager = new PageRequest(page, size, new Sort(Sort.Direction.valueOf(sortedOrder), STARTED_AT_STRING));
        // passing nulls for currency, payeePartyId and payeePartyIdType here to mean that the results should be restricted to the user's assignments
        Page<TransactionRequest> result = transactionRequestFilter(pager, specs, null, null, null);
        List<TransactionRequest> data = result.getContent();
        if (data.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ErrorResponse.Builder().setErrorCode("" + HttpServletResponse.SC_NOT_FOUND).setErrorDescription("Empty response").setDeveloperMessage("Empty response").build();
        }
        try {
            CsvUtility.writeToCsv(response, data);
        } catch (WriteToCsvException e) {
            return new ErrorResponse.Builder().setErrorCode(e.getErrorCode()).setErrorDescription(e.getErrorDescription()).setDeveloperMessage(e.getDeveloperMessage()).build();
        }
        return null;
    }

    /*
     * Returns respective [TransactionRequest] specifications based on filter
     * @param filter the filter we want to apply
     * @param listOfValues the values to which we want to apply filter
     */
    private Specifications<TransactionRequest> getFilterSpecs(Filter filter, List<String> listOfValues) {
        Specifications<TransactionRequest> spec = null;
        switch (filter) {
            case TRANSACTIONID:
                spec = TransactionRequestSpecs.in(TransactionRequest_.transactionId, listOfValues);
                break;
            case PAYERID:
                spec = TransactionRequestSpecs.in(TransactionRequest_.payerPartyId, listOfValues);
                break;
            case PAYEEID:
                spec = TransactionRequestSpecs.in(TransactionRequest_.payeePartyId, listOfValues);
                break;
            case WORKFLOWINSTANCEKEY:
                spec = TransactionRequestSpecs.in(TransactionRequest_.workflowInstanceKey, listOfValues);
                break;
            case STATE:
                spec = TransactionRequestSpecs.in(TransactionRequest_.state, parseStates(listOfValues));
                break;
            case ERRORDESCRIPTION:
                spec = TransactionRequestSpecs.filterByErrorDescription(parseErrorDescription(listOfValues));
                break;
            case EXTERNALID:
                spec = TransactionRequestSpecs.in(TransactionRequest_.externalId, listOfValues);
                break;

            case CLIENTCORRELATIONID:
                spec = TransactionRequestSpecs.in(TransactionRequest_.clientCorrelationId, listOfValues);
                break;
        }
        return spec;
    }

    /*
     * Parse the date filter and return the specification accordingly
     * @param startTo date before which we want all the records, in format "yyyy-MM-dd HH:mm:ss"
     * @param startFrom date after which we want all the records, in format "yyyy-MM-dd HH:mm:ss"
     */
    private Specifications<TransactionRequest> getDateSpecification(String startTo, String startFrom) throws Exception {
        if (startFrom != null && startTo != null) {
            return TransactionRequestSpecs.between(TransactionRequest_.startedAt, dateFormat().parse(startFrom), dateFormat().parse(startTo));
        } else if (startFrom != null) {
            return TransactionRequestSpecs.later(TransactionRequest_.startedAt, dateFormat().parse(startFrom));
        } else if (startTo != null) {
            return TransactionRequestSpecs.earlier(TransactionRequest_.startedAt, dateFormat().parse(startTo));
        } else {
            throw new Exception("Both dates(startTo, startFrom empty, skipping");
        }
    }

    /*
     * Executes the transactionRequest api request with specifications and returns the paged result
     * @param baseSpec the base specification in which all the other spec needed to be merged
     * @param extraSpecs the list of specification which is required to be merged in [baseSpec]
     * @param page the page number we want to fetch
     * @param size the size of single page or number of elements in single page
     * @param sortedOrder the order of sorting to be applied ASC OR DESC
     */
    private Page<TransactionRequest> executeRequest(Specifications<TransactionRequest> baseSpec, List<Specifications<TransactionRequest>> extraSpecs, int page, int size, String sortedOrder) {
        PageRequest pager = new PageRequest(page, size, new Sort(Sort.Direction.valueOf(sortedOrder), STARTED_AT_STRING));
        Page<TransactionRequest> result;
        if (baseSpec == null) {
            result = transactionRequestRepository.findAll(pager);
            logger.info("Getting data without spec");
        } else {
            Specifications<TransactionRequest> combineSpecs = combineSpecs(baseSpec, extraSpecs);
            result = transactionRequestRepository.findAll(combineSpecs, pager);
        }
        return result;
    }

    /*
     * Combines the multiple specifications into one using and clause
     * @param baseSpec the base specification in which all the other spec needed to be merged
     * @param specs the list of specification which is required to be merged in [baseSpec]
     */
    private <T> Specifications<T> combineSpecs(Specifications<T> baseSpec, List<Specifications<T>> specs) {
        logger.info("Combining specs {}", specs.size());
        for (Specifications<T> specifications : specs) {
            baseSpec = baseSpec.and(specifications);
        }
        return baseSpec;
    }

    /*
     * Generates the exhaustive errorDescription list by prefixing and suffixing it with double quotes (")
     *
     * Example: [ "AMS Local is disabled"] => [ "AMS Local is disabled", "\"AMS Local is disabled\""]
     */
    private List<String> parseErrorDescription(List<String> description) {
        List<String> errorDesc = new ArrayList<>(description);
        for (String s : description) {
            errorDesc.add(String.format("\"%s\"", s));
        }
        return errorDesc;
    }

    /*
     * Parses the [Filter] enum from filter string
     */
    private Filter parseFilter(String filterBy) {
        return filterBy == null ? null : Filter.valueOf(filterBy.toUpperCase());
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

    /*
     * Parses the list of [TransactionRequestState] enum from list of transactionState string
     */
    private List<TransactionRequestState> parseStates(List<String> states) {
        List<TransactionRequestState> stateList = new ArrayList<>();
        for (String state : states) {
            stateList.add(parseState(state));
        }
        return stateList;
    }
}
