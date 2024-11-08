    package com.seamless.kyc.actions

    import com.seamless.common.ExtendedProperties
    import com.seamless.common.StringUtils
    import com.seamless.common.config.ERSModuleConfiguration
    import com.seamless.ers.interfaces.ersifcommon.dto.KYCField
    import com.seamless.ers.interfaces.ersifcommon.dto.resellers.ResellerData
    import com.seamless.ers.interfaces.ersifextlink.dto.ERSResellerInfo
    import com.seamless.ers.kyc.client.constants.KycConstants
    import com.seamless.ers.kyc.client.model.*
    import com.seamless.kyc.config.KycSpringContext
    import com.seamless.kyc.interfaces.ActionEngineInterface
    import com.seamless.kyc.repository.CustomerV2Repository
    import com.seamless.kyc.repository.impl.CustomerV2RepositoryImpl
    import com.seamless.kyc.utils.ObjectToJSONUtil
    import org.apache.http.HttpStatus
    import org.slf4j.Logger
    import org.slf4j.LoggerFactory
    import org.springframework.beans.factory.annotation.Autowired
    import org.springframework.http.HttpEntity
    import org.springframework.http.HttpHeaders
    import org.springframework.http.HttpMethod
    import org.springframework.http.ResponseEntity
    import org.springframework.util.CollectionUtils
    import org.springframework.web.client.HttpClientErrorException
    import org.springframework.web.client.HttpServerErrorException
    import org.springframework.web.client.RestTemplate
    import org.springframework.web.client.UnknownHttpStatusCodeException
    import org.springframework.web.util.UriComponentsBuilder
    import se.seamless.ers.transaction.SearchResellerTransaction

    import static com.seamless.ers.kyc.client.constants.KycConstants.*

    class FallbackApprovalAction implements ActionEngineInterface {

        Logger log = LoggerFactory.getLogger(FallbackApprovalAction.class)
        @Autowired
        private ERSModuleConfiguration configuration

        List<String> kycExcludeStates = new ArrayList<>()

        @Override
        void processRequest(KycTransaction kycTransaction)
        {

            ExtendedProperties extendedProperties = configuration.getModuleProperties("businessactions.").getSubProperties("FallbackApprovalAction.")
            kycExcludeStates = Arrays.asList(configuration.getModuleProperties().getProperty("kyc.add.states.exclude", "").split(","));
            String approveId= extendedProperties.getProperty("approver.id", "")
            //String approveIdType = extendedProperties.getProperty("approver.id_type", "RESELLERID")
            String actionName = extendedProperties.get("className")
            log.info("Action started : {}", actionName)

            StageInfo stageInfo = initializeStageInfo(kycTransaction,actionName)
            setInitialResponse(kycTransaction)
            Set<String> failedActions = initActionAsFailed(kycTransaction, actionName)


            try
            {
                CustomerV2RepositoryImpl customerV2Repository = KycSpringContext.getBean(CustomerV2RepositoryImpl.class)

                //check if next level approval is set
                List<String> nextLevelApproval = kycTransaction.getCustomer().getNextLevelApprovals()

                if(nextLevelApproval != null && !nextLevelApproval.isEmpty())
                {

                    failedActions.removeIf {action -> (action == actionName) }
                    stageInfo.setStatus(true)
                    clearFailedAction(kycTransaction, failedActions)
                    log.info("Found next level approval: {}. No need for fallback approval.", nextLevelApproval.get(0))
                    return
                }

                log.info("No next level approval found. Will set next level approval to: {}", approveId)
                if (nextLevelApproval == null || nextLevelApproval == Collections.emptyList())
                {
                    kycTransaction.getCustomer().setNextLevelApprovals(new ArrayList<>())
                    log.info("Initialized next level approvals list")
                }

                kycTransaction.getCustomer().getNextLevelApprovals().add(approveId)

                failedActions.removeIf {action -> (action == actionName) }
                stageInfo.setStatus(true)
                clearFailedAction(kycTransaction, failedActions)

                log.info("List of next KYC approvals [{}] ", kycTransaction.getCustomer().getNextLevelApprovals())
                updateCustomer(kycTransaction.getCustomer(), customerV2Repository)

                log.info("Customer {} updated successfully for action {}", kycTransaction.getCustomer(), actionName)



            }
            catch (Exception exc)
            {
                log.error("Error while connecting to DMS : {}", exc.getMessage())
            }
        }

        private static StageInfo initializeStageInfo(KycTransaction kycTransaction, String actionName)
        {
            StageInfo stageInfo = new StageInfo()
            stageInfo.setTimeStamp(new Date())
            stageInfo.setActionName(actionName)
            stageInfo.setStatus(false)

            kycTransaction.getTransactionLifeCycle().getLifeCycleStages().add(stageInfo)
            return stageInfo
        }

        private static void setInitialResponse(KycTransaction kycTransaction)
        {
            kycTransaction.getBaseResponse().setResultCode(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultCode())
            kycTransaction.getBaseResponse().setResultMessage(KYCResultCodes.ACTION_FAILED_AT_KYC.getResultDescription())
        }

        private static Set<String> initActionAsFailed(KycTransaction kycTransaction, String actionName)
        {
            Set<String> failedActions = new HashSet<>()
            if (kycTransaction.getExtraFields().containsKey(ACTIONS_FAILED))
            {
                List<String> actionList = (List<String>) (Object) (Collections.singletonList(kycTransaction.getExtraFields().get(ACTIONS_FAILED)))
                failedActions = new HashSet<>(actionList)
                failedActions.add(actionName)
                kycTransaction.getExtraFields().replace(ACTIONS_FAILED, failedActions)
            }
            else
            {
                failedActions.add(actionName)
                kycTransaction.getExtraFields().put(ACTIONS_FAILED, failedActions)
            }

            return failedActions
        }

        private static Set<String> clearFailedAction(KycTransaction kycTransaction, Set<String> failedActions)
        {
            if (CollectionUtils.isEmpty(failedActions))
            {
                kycTransaction.getExtraFields().remove(ACTIONS_FAILED) as Set<String>
            }
            else
            {
                kycTransaction.getExtraFields().replace(ACTIONS_FAILED, failedActions) as Set<String>
            }
        }

        private  void updateCustomer(Customer currentCustomerDto, CustomerV2RepositoryImpl customerV2Repository) {
            Customer customer = customerV2Repository.findByCustomerIdAndKycStatus(currentCustomerDto.getCustomerId(), kycExcludeStates)
            if (customer != null)
            {
                log.info("Existing Customer record found with customerId = {}", customer.getCustomerId())
                customer.setEndDate(new Date())
                customer.setKycStatus((KYC_STATUS.DELETED).toString())
                customerV2Repository.index(customer, true)
                currentCustomerDto.setLastModifiedDate(new Date())
                customerV2Repository.index(currentCustomerDto, false)
            }
            else
            {
                log.warn("Customer {} not found in ES", currentCustomerDto.getCustomerId())
            }
        }


    }
