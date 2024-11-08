import com.fasterxml.jackson.databind.ObjectMapper
import com.seamless.common.ExtendedProperties
import com.seamless.common.StringUtils
import com.seamless.common.config.ERSModuleConfiguration
import com.seamless.ers.interfaces.ersifcommon.dto.AddressData
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter
import com.seamless.ers.interfaces.ersifcommon.dto.KYCField
import com.seamless.ers.interfaces.ersifcommon.dto.PrincipalId
import com.seamless.ers.interfaces.ersifcommon.dto.UserData
import com.seamless.ers.interfaces.ersifcommon.dto.resellers.ResellerData
import com.seamless.ers.interfaces.ersifcommon.dto.resellers.ResellerStatus
import com.seamless.ers.interfaces.ersifextlink.dto.ERSResellerInfo
import com.seamless.ers.interfaces.ersifextlink.dto.UpdateResellerProfileRequest
import com.seamless.ers.kyc.client.constants.KycConstants
import com.seamless.ers.kyc.client.model.KYCResultCodes
import com.seamless.ers.kyc.client.model.KycTransaction
import com.seamless.ers.kyc.client.model.StageInfo
import com.seamless.kyc.config.DataSourceConfig
import com.seamless.kyc.interfaces.ActionEngineInterface
import com.seamless.kyc.utils.ExpressionUtils
import com.seamless.kyc.utils.ObjectToJSONUtil
import freemarker.template.Template
import freemarker.template.TemplateException
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.UnknownHttpStatusCodeException

import static com.seamless.ers.kyc.client.constants.KycConstants.REQUEST
import static com.seamless.ers.kyc.client.constants.KycConstants.RESPONSE


class DMSUpdateResellerAction implements ActionEngineInterface {

    Logger log = LoggerFactory.getLogger(DMSUpdateResellerAction.class)

    private final static String TYPE_ID = "RESELLERID"
    private final static String PASSWORD = "motte_de_passe"
    private final static String CONTRACT_ID = "contractId"

    @Autowired
    private ERSModuleConfiguration configuration

    @Autowired
    DataSourceConfig dataSourceConfig


    private ObjectMapper objectMapper



    @Override
    void processRequest(KycTransaction kycTransaction) throws IOException, TemplateException {


        try {
            ExtendedProperties extendedProperties = configuration.getModuleProperties("businessactions.").getSubProperties("DMSUpdateResellerAction.")
            String actionName = extendedProperties.get("className")

            Set<String> failedActions = new HashSet<>()
            StageInfo stageInfo = new StageInfo()
            stageInfo.setTimeStamp(new Date())
            stageInfo.setActionName(actionName)
            stageInfo.setStatus(false)
            kycTransaction.getTransactionLifeCycle().getLifeCycleStages().add(stageInfo)
            kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultCode())
            kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultDescription())

           log.info("Action started : {}", actionName)

            objectMapper = dataSourceConfig.objectMapper()

            if (kycTransaction.getExtraFields().containsKey(KycConstants.ACTIONS_FAILED)) {
                List<String> actionList = (List<String>) (Object) (Arrays.asList(kycTransaction.getExtraFields().get(KycConstants.ACTIONS_FAILED)))
                failedActions = new HashSet<>((actionList))
                failedActions.add(actionName)
                kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, failedActions)
            } else {
                failedActions.add(actionName)
                kycTransaction.getExtraFields().put(KycConstants.ACTIONS_FAILED, failedActions)
            }

            Template template = ExpressionUtils.getTemplate(actionName, KycConstants.EXCLUDE_EXPRESSION, extendedProperties)
            if (template != null) {
                boolean criteriaMatched
                try {
                    criteriaMatched = ExpressionUtils.getValue(kycTransaction, template)
                } catch (Exception e) {
                    log.info("Got some exception while parsing template {}", e.getMessage())
                    return
                }
                if (criteriaMatched) {
                    failedActions.removeIf { action -> action.equals(actionName) }
                    stageInfo.setStatus(true)
                    if (CollectionUtils.isEmpty(failedActions)) {
                        kycTransaction.getExtraFields().remove(KycConstants.ACTIONS_FAILED)
                    } else {
                        kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, failedActions)
                    }
                    kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode())
                    kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.SUCCESS.getResultDescription())
                    log.info("criteria does not match for customer Id {}", kycTransaction.getCustomer().getCustomerId())
                    return
                }
            }

            log.info("Calling DMS to update reseller:")
            getResponseFromDms(kycTransaction, extendedProperties, stageInfo, failedActions, actionName)
        }
        catch (Exception exc) {
            log.error("Error while connecting to DMS : {}", exc.getMessage())
        }
    }

    def getResponseFromDms(KycTransaction kycTransaction, ExtendedProperties extendedProperties, StageInfo stageInfo, Set<String> failedActions, String actionName) {

        if (StringUtils.isNotEmpty(String.valueOf(extendedProperties.getProperty(KycConstants.URL)))) {
            Map<String, Object> dmsApiInfo = new HashMap<>()
            try {
                log.info("Getting DMS url")
                String url = extendedProperties.getProperty(KycConstants.URL)
                log.info("DMS url for operation {} URL: {}", kycTransaction.getOperationType(), url)
                ResponseEntity<?> response = null
                HttpEntity<?> requestData
                stageInfo.getApiInfo().put("DMS call to fetch group with userId", dmsApiInfo)
                dmsApiInfo.put(URL, url)
                try {
                    requestData = getDmsRequestBody(kycTransaction, extendedProperties.getProperty(KycConstants.PARENT_RESELLERID))
                    dmsApiInfo.put(REQUEST, requestData)
                    log.info("DMS request for operation {} resellerInfo : {}", kycTransaction.getOperationType(), ObjectToJSONUtil.toString(requestData))
                    //println("DMS request for operation " + kycTransaction.getOperationType() + " resellerInfo : " + ObjectToJSONUtil.toString(requestData))
                    RestTemplate restTemplate = new RestTemplate()
                    response = restTemplate.postForEntity(url, requestData, Object.class)
                    dmsApiInfo.put(RESPONSE, response)
                    log.info("DMS response for operation {} resellerInfo : {}", kycTransaction.getOperationType(), ObjectToJSONUtil.toString(response))
                    //println("DMS response for operation " + kycTransaction.getOperationType() + " resellerInfo : " + ObjectToJSONUtil.toString(response))
                } catch (HttpClientErrorException e) {
                    dmsApiInfo.put(RESPONSE, e)
                    String baseRestResponse = (String) e.getResponseBodyAsString()
                    log.error("Error while calling DMS Action Error : {} baseRestResponse : {}", e.getMessage(), baseRestResponse, e)
                } catch (HttpServerErrorException | UnknownHttpStatusCodeException e) {
                    dmsApiInfo.put(RESPONSE, e)
                    String baseRestResponse = (String) e.getResponseBodyAsString()
                    log.error("Error while calling DMS Action Error : {} baseRestResponse : {}", e.getMessage(), baseRestResponse, e)
                }

                if (response == null || response.getStatusCode().value() != HttpStatus.SC_OK) {
                    kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode())
                    kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription())
                    log.info("DMS failed for operation {} Reason: response null or invalid http status", kycTransaction.getOperationType())
                } else {
                    LinkedHashMap baseRestResponse = (LinkedHashMap) response.getBody()
                    log.info("Response from DMS for operation  {} Response:  {}", kycTransaction.getOperationType(), ObjectToJSONUtil.toString(baseRestResponse))
                    if (baseRestResponse != null && baseRestResponse.containsKey("resultCode") && baseRestResponse.get("resultCode") != null && Integer.parseInt(baseRestResponse.get("resultCode").toString()) == 0) {
                        log.info("DMS successful for operation {}", kycTransaction.getOperationType())
                        failedActions.removeIf { action -> action.equals(actionName) }
                        stageInfo.setStatus(true)
                        if (CollectionUtils.isEmpty(failedActions)) {
                            kycTransaction.getExtraFields().remove(KycConstants.ACTIONS_FAILED)
                        } else {
                            kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, failedActions)
                        }
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode())
                        kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.SUCCESS.getResultDescription())
                    } else if (baseRestResponse != null && baseRestResponse.containsKey("resultCode") && baseRestResponse.get("resultCode") != null) {
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode())
                        kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription())
                        log.info("DMS failed for operation {} Reason: invalid result code. Result code: {}", kycTransaction.getOperationType(), baseRestResponse.get("resultCode"))
                    } else {
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode())
                        kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription())
                        log.info("DMS failed for operation {}", kycTransaction.getOperationType())
                    }
                }
            } catch (Exception e) {
                dmsApiInfo.put(RESPONSE, e)
                kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode())
                kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription())
                log.error("Error while calling DMS for operation {} Error: {}", kycTransaction.getOperationType(), e.getMessage(), e)
            }
        } else {
            log.info("Skipping DMS. No DMS base url found")
        }
    }

    static def getDmsRequestBody(KycTransaction kycTransaction, String parentResellerId) {

        UpdateResellerProfileRequest request = new UpdateResellerProfileRequest()
        ERSResellerInfo dealerData = getResellerInfo(kycTransaction, parentResellerId)
        PrincipalId dealerPrincipal = getPrincipalId(kycTransaction)

        request.setDealerData(dealerData)
        request.setDealerPrincipal(dealerPrincipal)

        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        for (Map.Entry<String, String> entry : kycTransaction.getHeader().entrySet()) {
            headers.add(entry.getKey(), entry.getValue())
        }

        return new HttpEntity<>(request, headers)
    }

    static def getResellerInfo(KycTransaction kycTransaction, String parentResellerId) {
        ERSResellerInfo dealerData = new ERSResellerInfo()

        ResellerData resellerData = new ResellerData()
        resellerData.setResellerId(kycTransaction.getCustomer().getCustomerId())
        resellerData.setResellerMSISDN(kycTransaction.getCustomer().getMsisdn())
        resellerData.setParentResellerId(StringUtils.isNotEmpty(kycTransaction.getCustomer().getParentResellerId())
                ? kycTransaction.getCustomer().getParentResellerId() : parentResellerId)
        resellerData.setResellerName(kycTransaction.getCustomer().getFirstName())
        resellerData.setContractId(kycTransaction.getCustomer().getExtraFields().get(CONTRACT_ID))
        resellerData.setStatus(ResellerStatus.valueOf(kycTransaction.getCustomer().getCustomerStatus()))
        resellerData.setResellerTypeId(kycTransaction.getCustomer().getCustomerType())
        resellerData.setAddress(getAddress(kycTransaction))
        dealerData.setResellerData(resellerData)

        dealerData.setUsers(getUsers(kycTransaction))

        dealerData.setAdditionalFields(getAdditionalFieldObjects(kycTransaction.getCustomer().getExtraFields()))

        return dealerData
    }

    static def getAdditionalFieldObjects(Hashtable<String, String> extraFields) {
        List<KYCField> additionalFields = new ArrayList<>()
        extraFields.keySet().each { String key ->

            KYCField kycField = new KYCField()
            kycField.setName(key)
            kycField.setValue(extraFields.get(key))
            additionalFields.add(kycField)
        }
        return additionalFields
    }

    static def getAddress(KycTransaction kycTransaction) {
        AddressData address = new AddressData()
        address.setCity(kycTransaction.getCustomer().getCity())
        address.setEmail(kycTransaction.getCustomer().getEmail())
        address.setStreet(kycTransaction.getCustomer().getStreet())
        address.setZip(kycTransaction.getCustomer().getZipcode())
        return address
    }

    static def getPrincipalId(KycTransaction kycTransaction) {
        PrincipalId principalId = new PrincipalId()
        principalId.setId(kycTransaction.getCustomer().getCustomerId())
        principalId.setType(TYPE_ID)
        return principalId
    }

    private static def getUsers(KycTransaction kycTransaction) {
        List<UserData> users = new ArrayList<>()
        UserData userData = new UserData()
        userData.setUserId(kycTransaction.getCustomer().getCustomerId())
        userData.setPassword(kycTransaction.getCustomer().getExtraFields().get(PASSWORD))

        ERSHashtableParameter fields = new ERSHashtableParameter()
        Map<String, String> parameters = new HashMap<>()
        parameters.put("MSISDN", kycTransaction.getCustomer().getMsisdn())
        parameters.put("Email", kycTransaction.getCustomer().getEmail())
        fields.setParameters(parameters)
        userData.setFields(fields)
        users.add(userData)

        return users
    }
}