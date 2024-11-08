"DMS": {
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}"
    <#if response.resultDetails??>,
    "resultDetails":[<#list response.resultDetails?keys as key>
        {
           "${(key)!'N/A'}":"${(response.resultDetails[key])!'N/A'}"
        }
       <#sep>,
       </#list>]
    </#if>
}