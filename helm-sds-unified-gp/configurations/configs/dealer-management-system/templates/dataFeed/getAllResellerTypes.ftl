"DMS" :
{ "resultCode": ${response.resultCode?string.computer!400},
"resultDescription": "${response.resultDescription!""}",
"totalCount": ${response.totalCount!0},
"types":[<#list response.types as type>
       {
           "id":"${(type.id)!'N/A'}",
           "name":"${(type.name)!'N/A'}"
       }
       <#sep>,
       </#list>]
}