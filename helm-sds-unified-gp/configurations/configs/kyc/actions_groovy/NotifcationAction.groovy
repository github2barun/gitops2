import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.seamless.common.ERSConfigurationException;
import com.seamless.common.ExtendedProperties;
import com.seamless.common.config.ERSModuleConfiguration
import com.seamless.common.httpclient.RestClient;
import com.seamless.common.httpclient.RestClientResponse;
import com.seamless.common.ims.Exception.UnInitializedException;
import com.seamless.common.transaction.SystemToken;
import com.seamless.common.util.SystemTokenUtil;
import com.seamless.ers.interfaces.ersifcommon.dto.PrincipalId;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSGetResellerInfoRequest;
import com.seamless.ers.interfaces.ersifextlink.dto.ERSResellerInfo;
import com.seamless.ers.kyc.client.constants.KycConstants;
import com.seamless.ers.kyc.client.model.KYCResultCodes;
import com.seamless.ers.kyc.client.model.KycTransaction;
import com.seamless.ers.kyc.client.model.StageInfo;
import com.seamless.kyc.config.KycConfig;
import com.seamless.kyc.config.KycSpringContext;
import com.seamless.kyc.interfaces.ActionEngineInterface;
import com.seamless.kyc.utils.KycUtils;
import com.seamless.kyc.utils.ObjectToJSONUtil;

import freemarker.template.Configuration;
import com.seamless.one.groupmanagement.api.model.FindGroupResponseModel;
import com.seamless.one.groupmanagement.api.model.GroupMemberModel;
import com.seamless.one.groupmanagement.api.model.GroupMemberResponseModel;
import com.seamless.one.groupmanagement.api.model.GroupsResponseModel;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import com.seamless.kyc.actions.BulkResellerInfo;
import com.seamless.kyc.actions.NotificationRequest;
import com.seamless.kyc.actions.Fields;

import static com.seamless.kyc.utils.CommonUtils.localizeMessage;

public class NotifcationAction implements ActionEngineInterface
{
	Logger log = LoggerFactory.getLogger(NotifcationAction.class);

    private KycConfig kycConfig;

    private ExtendedProperties extendedProperties;
    private String actionName;
    List<String> list = new ArrayList<>();

    String resolveBulkResellerURL = "http://localhost:8033/dms/auth/getBulkResellerInfo";

	private TemplateEngine htmlTemplateEngine;

	@Autowired
	private ERSModuleConfiguration configuration;

	@Override
	public void processRequest(KycTransaction kycTransaction) throws IOException, TemplateException
	{
		actionName=kycTransaction.getExtraFields().get(KycConstants.ACTIVE_ACTION);
		log.info("Action groovy started : " + actionName);
		extendedProperties = configuration.getModuleProperties("businessactions.").getSubProperties(actionName+".");
		log.debug("extendedProperties " + extendedProperties);
		kycConfig=KycSpringContext.getBean(KycConfig.class);
		// get instance of (Spring Managed class)
        htmlTemplateEngine = KycSpringContext.getBean(TemplateEngine.class);
		StageInfo stageInfo = new StageInfo();
        stageInfo.setTimeStamp(new Date());
        stageInfo.setActionName(actionName);
        stageInfo.setStatus(false);
        kycTransaction.getTransactionLifeCycle().getLifeCycleStages().add(stageInfo);
        kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultCode());
        kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultDescription()));



        if(kycTransaction.getExtraFields().containsKey(KycConstants.ACTIONS_FAILED))
        {
            List<String> actionList = new ArrayList<>();
            actionList = (List<String>)(Object)(Arrays.asList(kycTransaction.getExtraFields().get(KycConstants.ACTIONS_FAILED)));
            list = new ArrayList<>(actionList);
            list.add(actionName);
            kycTransaction.getExtraFields().replace(KycConstants.ACTIONS_FAILED, list);
        }
        else
        {
            list.add(actionName);
            kycTransaction.getExtraFields().put(KycConstants.ACTIONS_FAILED, list);
        }

		boolean isAllow = false;
		String url = extendedProperties.getProperty("url", "http://localhost:8277/register");
		String allowTypeIdRelations = extendedProperties.getProperty("allow_type_id_relations", "");
		// type is type or id
		String matchPartnerType = extendedProperties.getProperty("match_partner_type", "InitiatorTypeToCustomerType");
		String template = extendedProperties.getProperty("template", "RegisterKYCEMAILNotificationTemplate");
		String from = extendedProperties.getProperty("from", "");
		String fromType = extendedProperties.getProperty("from_type", "");
		String to = extendedProperties.getProperty("to", "");
		String toType = extendedProperties.getProperty("to_type", "");
		String notificationType = extendedProperties.getProperty("notification_type", "SMS");
		resolveBulkResellerURL = extendedProperties.getProperty("bulk_reseller_url", "http://localhost:8033/dms/auth/getBulkResellerInfo");
		log.debug(url + ", " + allowTypeIdRelations + ", " + matchPartnerType + ", " + template +", " + from + ", " + fromType+", " + to + ", " + toType+", " + notificationType + ", " + resolveBulkResellerURL);
		isAllow = isAllowable(allowTypeIdRelations, matchPartnerType, kycTransaction);
		if(isAllow)
		{
			String sender = resolveContactInfo(from, fromType, kycTransaction, notificationType);
			String recipient = resolveContactInfo(to, toType, kycTransaction, notificationType);
			String bodyMessage = resolveBodyMessage(template, kycTransaction);
			String referenceEventId = template;
			if(StringUtils.isEmpty(sender))
			{
				log.error("Could not able to resolve sender " + kycTransaction.getOperationType());
				kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.EMAIL_ACTION_FAILED.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.EMAIL_ACTION_FAILED.getResultDescription()));
				return;
			}
			if(StringUtils.isEmpty(recipient))
			{
				log.error("Could not able to resolve recipient " + kycTransaction.getOperationType());
				kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.EMAIL_ACTION_FAILED.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.EMAIL_ACTION_FAILED.getResultDescription()));
				return;
			}
			if(StringUtils.isEmpty(bodyMessage))
			{
				log.error("Could not able to resolve bodyMessage " + kycTransaction.getOperationType());
				kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.EMAIL_ACTION_FAILED.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.EMAIL_ACTION_FAILED.getResultDescription()));
				return;
			}

			boolean isSend = sendNotification(url, KycUtils.generateSystemToken(kycTransaction.getUser()), sender, recipient, bodyMessage, referenceEventId, notificationType);

			if(isSend)
			{
				log.info("Notification send successfully " + kycTransaction.getOperationType());
				log.info("Sender : " + sender);
				log.info("Recipient : " + recipient);
				log.info("Message : " + bodyMessage);
				kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.SUCCESS.getResultDescription()));
                list.remove(actionName);
                stageInfo.setStatus(true);
			}
			else
			{
				log.error("Failed to send notification " + kycTransaction.getOperationType());
				log.error("Sender : " + sender);
				log.error("Recipient : " + recipient);
				log.error("Message : " + bodyMessage);
				kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.EMAIL_ACTION_FAILED.getResultCode());
                kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.EMAIL_ACTION_FAILED.getResultDescription()));
			}
		}
		else
		{
			log.info("Skipping this action");
			kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.SUCCESS.getResultCode());
            kycTransaction.getBaseResponse().setResultMessage(localizeMessage(KYCResultCodes.SUCCESS.getResultDescription()));
            list.remove(actionName);
            stageInfo.setStatus(true);
		}

	}

	def isAllowable(String allowTypeIdRelations, String matchPartnerType, KycTransaction kycTransaction)
	{
		String fromString = "";
		String toString = "";
		switch (matchPartnerType)
		{
			case "InitiatorTypeToCustomerType":
				fromString = kycTransaction.getCustomer().getCreaterType();
				toString = kycTransaction.getCustomer().getCustomerType();
				break;
			case "InitiatorIdToCustomerId":
				fromString = kycTransaction.getCustomer().getCreaterId();
				toString = kycTransaction.getCustomer().getCustomerId();
				break;
		}
		String relationKey = fromString + "->" + toString;
		return relationKey.matches(allowTypeIdRelations);
	}

	def resolveBodyMessage(String template, KycTransaction kycTransaction)
	{
		String language = kycTransaction.getLocale().getLanguage();
		template = template + "_" + language;
		log.info("Loading template : " + template);
		Context context = new Context();
		context.setVariable("kycTransaction", kycTransaction);
		String htmlContent = htmlTemplateEngine.process(template, context);
		return htmlContent;
	}

	def resolveContactInfo(String contactInfoExpressions, String contactInfoType, KycTransaction kycTransaction, String notificationType)
	{
		String [] contactInfoExpression = contactInfoExpressions.split(",");
		List<String> result = new ArrayList<>();
		if(contactInfoExpression != null && !contactInfoExpressions.isEmpty())
		{
			switch (contactInfoType)
			{
				case "ContactId":
					result = Arrays.asList(contactInfoExpression);
					break;
				case "ContactExpression":
					result = resolveContactExpression(contactInfoExpression, kycTransaction);
					break;
				case "ContactResellerIdExpression":
					result = resolveContactResellerIdExpression(contactInfoExpression, kycTransaction, notificationType);
					break;
				case "ContactGroupNameExpression":
					result = resolveContactGroupNameExpression(contactInfoExpression, kycTransaction, notificationType);
					break;
			}
		}

		if(CollectionUtils.isEmpty(result))
			return null;
		else
			return String.join(",", result);
	}

	def resolveContactGroupNameExpression(
			String[] contactInfoExpression,
			KycTransaction kycTransaction,
			String notificationType)
	{
		List<String> result = new ArrayList<>();
		List<String> groupNameList = new ArrayList<>();
		for(String expression : contactInfoExpression)
		{
			String groupName = resolveContactExpression(expression, kycTransaction);
			groupNameList.add(groupName);
		}

		if(!CollectionUtils.isEmpty(groupNameList))
		{
			HttpEntity<?> requestData = createGetGroupRequest(groupNameList, kycTransaction);
			log.debug("GMS request for operation " + kycTransaction.getOperationType() + " group request : " + ObjectToJSONUtil.toString(requestData));
			FindGroupResponseModel findGroupResponseModel = getGroupMemberDetailsByName(requestData,groupNameList);
			log.debug("GMS respone for operation " + kycTransaction.getOperationType() + " group response : " + ObjectToJSONUtil.toString(findGroupResponseModel));
			if(findGroupResponseModel.getGroup() != null && !CollectionUtils.isEmpty(findGroupResponseModel.getGroup().getMembers()))
			{
				for(GroupMemberModel groupMemberModel : findGroupResponseModel.getGroup().getMembers())
				{
					result.add(groupMemberModel.getUserId());
				}
			}
			if(!CollectionUtils.isEmpty(result))
			{
				return getContactInfoByResellerIdList(result, kycTransaction, notificationType);
			}
		}
		return result;
	}

	def getGroupMemberDetailsByName(HttpEntity<?> requestData, List<String> groupName)
	{
		try
		{
			String gmsUrl = kycConfig.getGmsConfig().get(KycConstants.GMS).getProperty(KycConstants.URL)+ "/" + kycConfig.getGmsConfig().get(KycConstants.GMS).getProperty(KycConstants.FIND_MEMBERS_BY_GROUP_NAME);
            Map<String, String> pathParams = new HashMap<String, String>();
            pathParams.put("groupname", groupName.get(0));
			ResponseEntity<FindGroupResponseModel> response = null;
			log.debug("GMS request for operation url " + gmsUrl +" requestData : " + ObjectToJSONUtil.toString(requestData));
			RestTemplate restTemplate = KycSpringContext.getBean(RestTemplate.class);
			response = restTemplate.exchange(gmsUrl, HttpMethod.GET, requestData, FindGroupResponseModel.class,pathParams);
			if (response == null || response.getStatusCode().value() != HttpStatus.SC_OK)
			{
                log.info("GMS action failed for action " +actionName + "Reason: response null or invalid http status");
            }
			else if (response.getBody() != null && response.getBody().getResultCode() != 0)
			{
                log.info("GMS failed for action " +actionName + " " + "Reason: invalid result code. Result code: " + response.getBody().getResultCode());
            }
			else
			{
				return response.getBody();
			}
		}

		catch (HttpClientErrorException e)
		{
            log.error("Error while calling DMS to fetch reseller info with type and location : " + e.getMessage(), e);
        }
		catch (HttpServerErrorException | UnknownHttpStatusCodeException e)
		{
            log.error("Error while calling DMS to fetch reseller info with type and location : " + e.getMessage(), e);
        }
		return null;
	}

	def createGetGroupRequest(List<String> groupNameList, KycTransaction kycTransaction)
	{

        HttpHeaders headers = new HttpHeaders();
        try {
            KycUtils kycUtils =new KycUtils(kycConfig);
            headers.add(KycConstants.SYSTEM_TOKEN, SystemTokenUtil.encode(KycUtils.generateSystemToken(kycTransaction.getUser()), kycConfig.getConfiguration().getModuleProperties()));
        } catch (Exception e) {
            log.error("Exception occurred while encoding token");
        }
        headers.add(KycConstants.AUTHORIZATION,"");

        return new HttpEntity(null,headers);
	}

	def resolveContactResellerIdExpression(String[] contactInfoExpression, KycTransaction kycTransaction, String notificationType)
	{
		List<String> result = new ArrayList<>();
		List<String> resellerIdList = new ArrayList<>();
		for(String expression : contactInfoExpression)
		{
			String resellerId = resolveContactExpression(expression, kycTransaction);
			resellerIdList.add(resellerId);
		}

		return getContactInfoByResellerIdList(resellerIdList, kycTransaction, notificationType);
	}

	def getContactInfoByResellerIdList(List<String> resellerIdList, KycTransaction kycTransaction, String notificationType)
	{
		List<String> result = new ArrayList<>();

		if(!CollectionUtils.isEmpty(resellerIdList))
		{
			HttpEntity<?> requestData = createRequest(resellerIdList, kycTransaction);
			log.debug("DMS request for operation " + kycTransaction.getOperationType() + " resellerInfo : " + ObjectToJSONUtil.toString(requestData));
			BulkResellerInfo bulkResellerInfo = getBulkResellerInfo(requestData);
			if(bulkResellerInfo == null)
				return result;
			if("EMAIL".equals(notificationType))
				result.addAll(getEmailIds(bulkResellerInfo));
			else if("SMS".equals(notificationType))
				result.addAll(getMSISDNList(bulkResellerInfo));
		}

        return result;
	}

	def getMSISDNList(BulkResellerInfo bulkResellerInfo)
	{
		List<String> msisdnList = new ArrayList<>();
		if (!CollectionUtils.isEmpty(bulkResellerInfo.getResellers()))
		{
			for(ERSResellerInfo ersResellerInfo : bulkResellerInfo.getResellers())
			{
				if(ersResellerInfo.getResellerData()!=null
						&& !StringUtils.isEmpty(ersResellerInfo.getResellerData().getResellerMSISDN()))
				{
					String msisdn = ersResellerInfo.getResellerData().getResellerMSISDN();
					msisdnList.add(msisdn);
				}
			}
		}
		return msisdnList;
	}

	def getEmailIds(BulkResellerInfo bulkResellerInfo)
	{
		List<String> emailIds = new ArrayList<>();
		if (!CollectionUtils.isEmpty(bulkResellerInfo.getResellers()))
		{
			for(ERSResellerInfo ersResellerInfo : bulkResellerInfo.getResellers())
			{
				if(ersResellerInfo.getResellerData()!=null
						&& ersResellerInfo.getResellerData().getAddress() != null
						&& !StringUtils.isEmpty(ersResellerInfo.getResellerData().getAddress().getEmail()))
				{
					String email = ersResellerInfo.getResellerData().getAddress().getEmail();
					emailIds.add(email);
				}
			}
		}
		return emailIds;
	}

	def getBulkResellerInfo(HttpEntity<?> requestData)
	{
		try
		{
			ResponseEntity<BulkResellerInfo> response = null;
			RestTemplate restTemplate = KycSpringContext.getBean(RestTemplate.class);
			response = restTemplate.postForEntity(resolveBulkResellerURL, requestData, BulkResellerInfo.class);

			if (response == null || response.getStatusCode().value() != HttpStatus.SC_OK)
			{
                log.info("DMS action failed for action " +actionName + "Reason: response null or invalid http status");
            }
			else if (response.getBody() != null && response.getBody().getResultCode() != 0)
			{
                log.info("DMS failed for action " +actionName + " " + "Reason: invalid result code. Result code: " + response.getBody().getResultCode());
            }
			else
			{
				return response.getBody();
			}
		}

		catch (HttpClientErrorException e)
		{
            log.error("Error while calling DMS to fetch reseller info with type and location : " + e.getMessage(), e);
        }
		catch (HttpServerErrorException | UnknownHttpStatusCodeException e)
		{
            log.error("Error while calling DMS to fetch reseller info with type and location : " + e.getMessage(), e);
        }
		return null;
	}

	def createRequest(List<String> resellerIdList, KycTransaction kycTransaction)
	{
		List<ERSGetResellerInfoRequest> request = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        try {
            KycUtils kycUtils =new KycUtils(kycConfig);
            headers.add(KycConstants.SYSTEM_TOKEN, SystemTokenUtil.encode(KycUtils.generateSystemToken(kycTransaction.getUser()), kycConfig.getConfiguration().getModuleProperties()));
        } catch (Exception e) {
            log.error("Exception occurred while encoding token");
        }
        headers.add(KycConstants.AUTHORIZATION,"");

        for(String resellerId : resellerIdList)
        {
        	ERSGetResellerInfoRequest infoRequest =new ERSGetResellerInfoRequest();
        	PrincipalId principalId = new PrincipalId();
        	principalId.setId(resellerId);
        	principalId.setType("RESELLERID");
			infoRequest.setDealerID(principalId);

			request.add(infoRequest);
        }

        return new HttpEntity(request,headers);
	}

	def resolveContactExpression(String[] contactInfoExpression, KycTransaction kycTransaction)
	{
		List<String> result = new ArrayList<>();
		for(String expression : contactInfoExpression)
		{
			String tempResult = resolveContactExpression(expression, kycTransaction);
			if(tempResult != null && !tempResult.isEmpty())
				result.add(tempResult);
		}
		return result;
	}

	def resolveContactExpression(String expression, KycTransaction kycTransaction)
	{
		Configuration freemarkerConfig = new Configuration();
		String result = null;

		HashMap<String, KycTransaction> map = new HashMap<String, KycTransaction>();
		map.put("kycTransaction",kycTransaction);

		try
		{
			Template tagExpression = new Template("kyc_expression", new StringReader(expression), freemarkerConfig);
			StringWriter out = new StringWriter();
			tagExpression.process(map, out);
			result = out.toString();
		}
		catch (TemplateException | IOException e)
		{
			log.error("Failed to resolve contact expression for : " + expression);
			log.error(e.getMessage());
		}
		return result;
	}

	def sendNotification(String url, SystemToken systemToken, String senderId, String recipientId, String message, String referenceEventId, String notificationType)
	{
		//RestClient restClient = new RestClient.Builder(url).build(kycConfig.getConnectionProperties());
		RestTemplate restTemplate = KycSpringContext.getBean(RestTemplate.class);

		NotificationRequest request = new NotificationRequest();
		request.setAuthorization("eyJ0eXAiOi");
		request.setEventTag("ADHOC_ALERT");
		request.setSystemToken(systemToken);
		Fields fields = new Fields();
		fields.setMessage(message);
		fields.setSenderId(senderId);
		fields.setRecipientId(recipientId);
		fields.setNotificationType(notificationType);
		request.setFields(fields);

		HttpEntity<NotificationRequest> httpEntity = new HttpEntity<>(request,
				getHttpHeaders(request.getSystemToken(), request.getAuthorization()));
		log.info("Request for notification action : " + ObjectToJSONUtil.toString(httpEntity));
		ResponseEntity<String> clientResponse = restTemplate.postForEntity(url, httpEntity, String.class);
		log.info("Response for notification action : " + ObjectToJSONUtil.toString(clientResponse));

		if (clientResponse != null && clientResponse.getBody() != null && "SUCCESS".equals(clientResponse.getBody()))
		{
			log.info("Message send with result code " + 0 + " with response body " + clientResponse);
			return true;
		}
		else
		{
			log.error("Error occur in message send with result code " + clientResponse.getBody());
			return false;
		}
	}

	def getHttpHeaders(SystemToken systemToken, String authorization)
	{
		String json=null;
		try
		{
			json = SystemTokenUtil.encode(systemToken, kycConfig.getConfiguration().getModuleProperties());
		}
		catch (InvalidKeyException | ERSConfigurationException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
				| NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | UnInitializedException e)
		{
			log.error("Failed to parse system token " + e.getMessage());
		}
		authorization = StringUtils.isBlank(authorization) ? "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9" : authorization;
		log.info("System-Token : " + json);
		log.info("authorization : " + authorization);

		HttpHeaders headers = new HttpHeaders();
		headers.set("accept", "application/json");
		headers.set("system-token", json);
		headers.set("authorization", authorization);

		return headers;
	}

}