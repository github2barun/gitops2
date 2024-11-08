"ams.ersReference": "${response.getErsReference()!""}",
"ams.resultcode": ${response.response.resultCode!400},
"ams.resultMessage": "${response.response.resultDescription!""}"<#if response.response.getAccountData()?? && response.response.getAccountData()?size gt 1>,</#if>
<#compress>
    <#if response.response.getAccountData()?? && response.response.getAccountData()?size gt 1>
        <#assign acctData = response.response.getAccountData() />
        "ams.accounts": [
        <#list acctData as data>
            <#if data.getAccount()??>
                "ams.account": "${data.getAccount().getAccountId()!""}",
                "ams.getAccountTypeId": "${data.getAccount().getAccountTypeId()!""}",
            </#if>
            "ams.accountStatus": "${data.getStatus()!""}",
            "ams.accountBalance": "${data.getBalance()!""}",
            "ams.isLoanEnabled": "${data.isLoanEnabled()?string('yes','no')}"<#if data_has_next>,</#if>
        </#list>
        ]
    <#else>
    </#if>
</#compress>