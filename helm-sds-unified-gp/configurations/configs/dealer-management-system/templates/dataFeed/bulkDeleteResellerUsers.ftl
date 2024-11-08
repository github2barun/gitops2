"DMS":
[<#list response.deleteUserTransactions as response>
    {
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}",
    "batchId": "${response.batchId!""}"
    "resellerId": "${response.resellerId!""}"
    <#if response.users??>,
    "users":[<#list response.users as user>
                {
                "userId":"${(user.userId)!'N/A'}",
                "name":"${(user.name)!'N/A'}",
                "email":"${(user.email)!'N/A'}"
                "phone":"${(user.phone)!'N/A'}"
                "roleId":"${(user.roleId)!'N/A'}"
                }
                <#sep>,
            </#list>]
    </#if>
    }
    <#sep>,
</#list>]