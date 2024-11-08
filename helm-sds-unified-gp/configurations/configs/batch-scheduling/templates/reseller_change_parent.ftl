{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    "resellers": [
    <#list request.requestObjectsList as resellerUserList>
        {
        "recordId": ${resellerUserList?counter},
        "resellerId": "${(resellerUserList.resellerId)!''}",
        "newResellerParentId": "${(resellerUserList.newResellerParentId)!''}"

        }<#if resellerUserList_has_next>,</#if>
    </#list>
    ]
</#compress>
}