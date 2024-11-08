"ams.ersReference": "${response.getErsReference()!""}",
"ams.resultCode": ${response.response.resultCode!400},
"ams.resultMessage": "${response.response.resultDescription!""}"<#if response.response.loanDetails?has_content>, </#if>
<#compress>
    <#if (response.response.loanDetails)??>
        <#assign loanDetails=response.response.getLoanDetails()/>
        "ams.resellerId": "${loanDetails.getResellerId()!""}",
        "ams.resellerType": "${loanDetails.getResellerType()!""}",
        "ams.loanAmount": "${loanDetails.getLoanAmount()!""}",
        "ams.paymentAmount": "${loanDetails.getPaymentAmount()!""}",
        "ams.remainingLoanAmount": "${loanDetails.getRemainingLoanAmount()!""}",
        "ams.loanDate": "${loanDetails.getLoanDate()?datetime?string("yyyy-MM-dd HH:mm:ss")!""}",
        "ams.dueDate": "${loanDetails.getDueDate()?datetime?string("yyyy-MM-dd HH:mm:ss")!""}",
        "ams.loanFullPaymentStatus": "${loanDetails.getLoanFullPaymentStatus()!""}"
    <#else>
    </#if>
</#compress>