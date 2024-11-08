{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
  <#if request.name??>
        "name": "${request.name}",
    </#if>
    "resellers": [
    <#list request.requestObjectsList as resellerList>
    {
        "recordId": ${resellerList?counter},
        "isAutoTransfer":"0",
        "address": {
            <#list resellerList.address?keys as key>
                "${key}" : <#if (resellerList.address[key]?? && resellerList.address[key] != "" )>"${(resellerList.address[key])}"<#else>null</#if>
                <#if key_has_next>,</#if>
            </#list>
        },
        "extraParams": {
            "parameters": {
                <#list resellerList.extraParams?keys as key>
                    "${key}" : <#if (resellerList.extraParams[key]?? && resellerList.extraParams[key] != "" )>"${(resellerList.extraParams[key])}"<#else>null</#if>
                    <#if key_has_next>,</#if>
                </#list>
            }
        },
        "users": [
            <#list resellerList.user?values as userList>
            {
            <#list userList?keys as key>
                <#if (userList[key]?? && userList[key] != "" )>
                    "${key}" : "${(userList[key])}"<#if key_has_next>,</#if>
                </#if>
            </#list>
            }
                <#if userList_has_next>,</#if></#list>
        ],
        <#list resellerList.fields?keys as key>
            "${key}" : <#if (resellerList.fields[key]?? && resellerList.fields[key] != "" )>"${(resellerList.fields[key])}"<#else>null</#if>
                <#if key_has_next>,</#if>
        </#list>
    }<#if resellerList_has_next>,</#if>
    </#list>
    ]
</#compress>
}