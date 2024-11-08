import com.fasterxml.jackson.databind.ObjectMapper
import com.seamless.common.ExtendedProperties
import com.seamless.common.StringUtils
import com.seamless.common.config.ERSModuleConfiguration
import com.seamless.ers.interfaces.ersifcommon.dto.ERSHashtableParameter
import com.seamless.ers.interfaces.ersifcommon.dto.AddressData
import com.seamless.ers.interfaces.ersifcommon.dto.PrincipalId
import com.seamless.ers.interfaces.ersifcommon.dto.resellers.ResellerStatus
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
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.UnknownHttpStatusCodeException

import static com.seamless.ers.kyc.client.constants.KycConstants.REQUEST
import static com.seamless.ers.kyc.client.constants.KycConstants.RESPONSE
import static com.seamless.ers.kyc.client.constants.KycConstants.URL
import static com.seamless.kyc.utils.CommonUtils.localizeMessage
import static com.seamless.kyc.enums.PasswordMaskSubstitutionTypes.*


public class DMSAction implements ActionEngineInterface {

    Logger log = Logger.getLogger(DMSAction.class);

    @Autowired
    private ERSModuleConfiguration configuration;

    @Autowired
    DataSourceConfig dataSourceConfig;

    List<String> list = new ArrayList<>();

    private String actionName;
    private String moduleName;
    private ObjectMapper objectMapper;


    @Override
    void processRequest(KycTransaction kycTransaction) throws IOException, TemplateException {
        ExtendedProperties extendedProperties;
        StageInfo stageInfo=new StageInfo();
        stageInfo.setTimeStamp(new Date());
        stageInfo.setActionName(actionName);
        stageInfo.setStatus(false);
        kycTransaction.getTransactionLifeCycle().getLifeCycleStages().add(stageInfo);
        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultCode());
        kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultDescription()));

        try {
            extendedProperties = configuration.getModuleProperties("businessactions.").getSubProperties("DMSAction.");
            actionName = extendedProperties.get("className");
            objectMapper = dataSourceConfig.objectMapper();

            if (kycTransaction.getExtraFields().containsKey(KycConstants.ACTIONS_FAILED)) {
                list = (List<String>) (Object) (Arrays.asList(kycTransaction.getExtraFields().get(KycConstants.ACTIONS_FAILED)));
                if(!list.contains(actionName))
                    list.add(actionName);
                kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, list);
            } else {
                if(!list.contains(actionName))
                    list.add(actionName);
                kycTransaction.getExtraFields().put(KycConstants.ACTIONS_FAILED, list);
            }

            Template template = ExpressionUtils.getTemplate(actionName, KycConstants.EXCLUDE_EXPRESSION, extendedProperties);
            if (template != null) {
                boolean criteriaMatched=false;
                try {
                    criteriaMatched=ExpressionUtils.getValue(kycTransaction,template);
                } catch (Exception e) {
                    log.info("Got some exception while parsing template " + e.getMessage());
                    return;
                }
                if (criteriaMatched) {
                    list.remove(actionName);
                    stageInfo.setStatus(true);
                    if(list.size()==0)
                    {
                        kycTransaction.getExtraFields().remove(KycConstants.ACTIONS_FAILED);
                    }
                    else {
                        kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, list);
                    }
                    kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode());
                    kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.SUCCESS.getResultDescription()));
                    log.info("criteria does not match for customer Id " + kycTransaction.getCustomer().getCustomerId());
                    return;
                }
            }

            log.info("Calling DMS :");
            getResponseFromDms(kycTransaction, extendedProperties,stageInfo);
        }
        catch (Exception exc) {
            log.error("Error while connecting to DMS : " + exc.getMessage());
        }
    }

    def getResponseFromDms(KycTransaction kycTransaction, ExtendedProperties extendedProperties,StageInfo stageInfo) {
        if (StringUtils.isNotEmpty(String.valueOf(extendedProperties.getProperty(KycConstants.URL)))) {
            Map<String,Object> dmsApiInfo=new HashMap<>();
            try {
                log.info("Getting DMS url");
                String url = extendedProperties.getProperty(KycConstants.URL);
                log.info("DMS url for operation " + kycTransaction.getOperationType() + " " + "URL: " + url);
                ResponseEntity<?> response = null;
                HttpEntity<?> requestData;
                stageInfo.getApiInfo().put("DMS call to fetch group with userId",dmsApiInfo);
                dmsApiInfo.put(URL,url);
                try {
                     requestData = getDmsRequestBody(kycTransaction, extendedProperties.getProperty(KycConstants.PARENT_RESELLERID));
                    dmsApiInfo.put(REQUEST,requestData);

                    String requestDataString = ObjectToJSONUtil.toStringWithPasswordMasking(requestData, PASSWORD_WITH_COLON_QUOTED, MOTTE_DE_PASS_NAME_VALUE_PAIR_QUOTED)
                    log.info("DMS request for operation " + kycTransaction.getOperationType() + " resellerInfo : " + requestDataString);
                    println("DMS request for operation " + kycTransaction.getOperationType() + " resellerInfo : " + requestDataString);
                    RestTemplate restTemplate = new RestTemplate();
                    response = restTemplate.postForEntity(url, requestData, Object.class);
                    dmsApiInfo.put(RESPONSE,response);

                    String responseDataString = ObjectToJSONUtil.toStringWithPasswordMasking(response, PASSWORD_WITH_COLON_QUOTED, MOTTE_DE_PASS_NAME_VALUE_PAIR_QUOTED)
                    println("DMS response for operation " + kycTransaction.getOperationType() + " resellerInfo : " + responseDataString);
                } catch (HttpClientErrorException e) {
                    dmsApiInfo.put(RESPONSE,e);
                    String baseRestResponse = (String) e.getResponseBodyAsString();
                    log.error("Error while calling DMS Action Error : " + e.getMessage() + " baseRestResponse : " + baseRestResponse, e);
                } catch (HttpServerErrorException | UnknownHttpStatusCodeException e) {
                    dmsApiInfo.put(RESPONSE,e);
                    String baseRestResponse = (String) e.getResponseBodyAsString();
                    log.error("Error while calling DMS Action Error : " + e.getMessage() + " baseRestResponse : " + baseRestResponse, e);
                }

                if (response == null || response.getStatusCode().value() != HttpStatus.SC_OK) {
                    kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode());
                    kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription()));
                    log.info("DMS failed for operation " + kycTransaction.getOperationType() + " " + "Reason: response null or invalid http status");
                } else {
                    LinkedHashMap baseRestResponse = (LinkedHashMap) response.getBody();
                    log.info("Response from DMS for operation  " + kycTransaction.getOperationType() + " Response:  " + ObjectToJSONUtil.toString(baseRestResponse));
                    if (baseRestResponse != null && baseRestResponse.containsKey("resultCode") && baseRestResponse.get("resultCode") != null && Integer.parseInt(baseRestResponse.get("resultCode").toString()) == 0) {
                        log.info("DMS successful for operation " + kycTransaction.getOperationType());
                        list.remove(actionName);
                        stageInfo.setStatus(true);
                        if(list.size()==0)
                        {
                            kycTransaction.getExtraFields().remove(KycConstants.ACTIONS_FAILED);
                        }
                        else {
                            kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, list);
                        }
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode());
                        kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.SUCCESS.getResultDescription()));
                    } else if (baseRestResponse != null && baseRestResponse.containsKey("resultCode") && baseRestResponse.get("resultCode") != null) {
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode());
                        if(response.getBody()["resultDescription"] !=null)
                            kycTransaction.getBaseResponse().setResultMessage(response.getBody()["resultDescription"] as String);
                        else
                            kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription()));
                        log.info("DMS failed for operation " + kycTransaction.getOperationType() + " " + "Reason: invalid result code. Result code: " + baseRestResponse.get("resultCode"));
                    } else {
                        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode());
                        kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription()));
                        log.info("DMS failed for operation " + kycTransaction.getOperationType());
                    }
                }
            } catch (Exception e) {
                dmsApiInfo.put(RESPONSE,e);
                kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.RESELLER_CREATION_ACTION_FAILED.getResultDescription()));
                log.error("Error while calling DMS for operation " + kycTransaction.getOperationType() + " " + " Error: " + e.getMessage(), e);
            }
        } else {
            log.info("Skipping DMS. No DMS base url found");
        }
    }

    def getDmsRequestBody(KycTransaction kycTransaction, String parentResellerId) {

        AddressData address = getAddress(kycTransaction);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        for (Map.Entry<String, String> entry : kycTransaction.getHeader().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("resellerType", kycTransaction.getCustomer().getCustomerType());
        body.add("resellerId", kycTransaction.getCustomer().getCustomerId());
        body.add("resellerMSISDN", kycTransaction.getCustomer().getMsisdn());
        body.add("parentResellerId", (StringUtils.isNotEmpty(kycTransaction.getCustomer().getParentResellerId())) ?
                kycTransaction.getCustomer().getParentResellerId() : parentResellerId);

        String status = kycTransaction.getCustomer().getCustomerStatus();
        body.add("status", StringUtils.isNotEmpty(status) ? status : ResellerStatus.Active.toString());
        body.add("JuridicalName", kycTransaction.getCustomer().getFamilyName());
        body.add("address", objectMapper.writeValueAsString(address));
        body.add("name", kycTransaction.getCustomer().getFirstName());
        body.add("countryCode", kycTransaction.getCustomer().getCountry());
        body.add("language", kycTransaction.getCustomer().getPreferredLanguage());
        body.add("contractId", kycTransaction.getCustomer().getExtraFields().get("contractId"));

        Hashtable<String, String> kycCustomerExtraFields = kycTransaction.getCustomer().getExtraFields();
        removeDuplicatesFrmAdditionalFields(body,kycCustomerExtraFields);

        List<UserData> userDataList = new ArrayList<>();
        UserData userData = new UserData();
        if(null != kycTransaction.getCustomer().getExtraFields().get("motte_de_passe")) {
            userData.setPassword(kycTransaction.getCustomer().getExtraFields().get("motte_de_passe"));
        }
        if(null != kycTransaction.getCustomer().getExtraFields().get("roleId")) {
            userData.setRoleId(kycTransaction.getCustomer().getExtraFields().get("roleId"));
        }

        ERSHashtableParameter parameters = new ERSHashtableParameter();
        Map<String, String> keyValue = new HashMap<>();
        if(null != kycTransaction.getCustomer().getExtraFields().get("roleStartDate")) {
            keyValue.put("roleStartDate",kycTransaction.getCustomer().getExtraFields().get("roleStartDate"));
        }
        if(null != kycTransaction.getCustomer().getExtraFields().get("roleExpiryDate")) {
            keyValue.put("roleExpiryDate",kycTransaction.getCustomer().getExtraFields().get("roleExpiryDate"));
        }
        parameters.setParameters(keyValue);
        userData.setFields(parameters);
        userDataList.add(userData);
        body.add("users",userDataList);

        if (kycCustomerExtraFields != null && kycCustomerExtraFields.size() > 0) {
            List<Map<String, Object>> additionalFields = getAdditionalFieldObjects(kycCustomerExtraFields)

            body.add("additionalFields", objectMapper.writeValueAsString(additionalFields));
        } else {
            log.info("No additionalFields defined");
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return request;
    }

    def getAdditionalFieldObjects(Hashtable<String, String> extraFields) {
        List<Map<String, Object>> additionalFields = new ArrayList<>();
        extraFields.keySet().each { String key ->
            if (key.contains("roleId") || key.contains("roleStartDate") || key.contains("roleExpiryDate")) {
                //skip unnecessary extra fields to send DMS
            } else {
                Map<String, Object> fieldsMap = new HashMap<>();
                fieldsMap.put("name", key);
                fieldsMap.put("value", extraFields.get(key));
                additionalFields.add(fieldsMap);
            }
        }
        return additionalFields;
    }

    def getAddress(KycTransaction kycTransaction) {
        AddressData address = new AddressData();
        address.setCity(kycTransaction.getCustomer().getCity());
        address.setCountry(kycTransaction.getCustomer().getCountry());
        address.setEmail(kycTransaction.getCustomer().getEmail());
        address.setPhone(kycTransaction.getCustomer().getTelNo1());
        address.setStreet(kycTransaction.getCustomer().getStreet());
        address.setZip(kycTransaction.getCustomer().getZipcode());
        return address;
    }

    def getPrincipalIds(KycTransaction kycTransaction) {
        List<PrincipalId> principalIds = new ArrayList<>();
        PrincipalId principalId = new PrincipalId();
        principalId.setId(kycTransaction.getUser().getUserId());
        principalId.setType(kycTransaction.getUser().getResellerIdType());
        principalIds.add(principalId);
        return principalIds;
    }

    def removeDuplicatesFrmAdditionalFields(LinkedMultiValueMap<String, String> requestBodyObj,Hashtable<String, String> kycCustomerExtraFields) {
        for (String key : requestBodyObj.keySet()) {
            kycCustomerExtraFields.remove(key);
        }
    }
}

class UserData
{
    private String password;
    private String roleId;
    private ERSHashtableParameter fields;

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getRoleId()
    {
        return roleId;
    }

    public void setRoleId(String roleId)
    {
        this.roleId = roleId;
    }

    public ERSHashtableParameter getFields()
    {
    	return fields;
    }

    public void setFields(ERSHashtableParameter fields)
    {
    	this.fields = fields;
    }
}