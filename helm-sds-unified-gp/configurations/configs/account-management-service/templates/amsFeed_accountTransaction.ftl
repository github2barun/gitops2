"ams.resultCode": ${response.response.resultCode!400},
"ams.resultMessage": "${response.response.resultDescription!""}" <#if response.response?has_content>, </#if>

<#compress>
    <#if response.response??>
        <#assign transaction=response.response/>

       "ams.accountTransaction": {
        	"ams.originalErsRreference": "${transaction.getReference()!""}",

        	 <#if transaction.getBalanceBefore()??>
            	"ams.balanceBefore":"${transaction.getBalanceBefore()}",
             </#if>
             <#if transaction.getBalanceAfter()??>
            	"ams.balanceAfter":"${transaction.getBalanceAfter()}"
             </#if>
            <#if transaction.getAccountClassId()?has_content>, </#if>
			 <#if transaction.getAccountClassId()??>
		            "ams.transactionType":"${transaction.getAccountClassId()}"
		     </#if>
		    <#if  transaction.getAccountData()?has_content>, </#if>

		    <#if transaction.getAccountData()?? && transaction.getAccountData().getAccount()??>
          	  <#assign account=transaction.getAccountData().getAccount()>
            	"ams.account":"${account.getAccountId()}",
            	"ams.accountTypeId":"${account.getAccountTypeId()}"
            </#if>
            <#if transaction.loanDetails?has_content>, </#if>

             <#if (transaction.loanDetails)??>
              "ams.loanDetails":{
			    <#assign loanDetails=transaction.getLoanDetails()/>
			    "ams.resellerId": "${loanDetails.getResellerId()!""}",
			    "ams.resellerType": "${loanDetails.getResellerType()!""}",
			    "ams.loanAmount": "${loanDetails.getLoanAmount()!""}",
			    "ams.paymentAmount": "${loanDetails.getPaymentAmount()!""}",
			    "ams.remainingLoanAmount": "${loanDetails.getRemainingLoanAmount()!""}",
			    "ams.loanDate": "${loanDetails.getLoanDate()?datetime?string("yyyy-MM-dd HH:mm:ss")!""}",
			    "ams.dueDate": "${loanDetails.getDueDate()?datetime?string("yyyy-MM-dd HH:mm:ss")!""}",
			    "ams.loanFullPaymentStatus": "${loanDetails.getLoanFullPaymentStatus()!""}"
			    }
			<#else>
			</#if>
          }
    </#if>

</#compress>