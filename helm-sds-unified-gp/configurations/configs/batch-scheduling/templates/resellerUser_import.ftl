{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    "users": [
    <#list request.requestObjectsList as resellerUserList>
        {
        "email": "${(resellerUserList.email)!''}",
        "name": "${(resellerUserList.name)!''}",
        "password": "${(resellerUserList.password)!''}",
        "phone": "${(resellerUserList.phone)!''}",
        "recordId": ${(resellerUserList.recordId)!''},
        "resellerId": "${(resellerUserList.resellerId)!''}",
        "roleId": "${(resellerUserList.roleId)!''}",
        "userId": "${(resellerUserList.userId)!''}"

        }<#if resellerUserList_has_next>,</#if>
    </#list>
    ]
</#compress>
}