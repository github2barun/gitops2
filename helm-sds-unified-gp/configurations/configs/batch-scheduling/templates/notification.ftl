{
<#compress>
    "notification": {
        "eventTag": "GENERIC_ALERT",
<#switch request.notificationData.getChannel()>
  <#case "SMS">
        "smsTemplateName": "${request.notificationData.getTemplateName()}",
        "smsTemplateMessage": "${(request.notificationData.getTemplateMessage()!"")?json_string}",
        "msisdn": "<#list request.requestObjectsList as recipientList>${recipientList.recipient}<#sep>;</#list>",
    <#break>
  <#case "EMAIL">
        "emailTemplateName": "${request.notificationData.getTemplateName()}",
        "emailTemplateMessage": "${(request.notificationData.getTemplateMessage()!"")?json_string}",
        "email": "<#list request.requestObjectsList as recipientList>${recipientList.recipient}<#sep>;</#list>",
    <#break>
</#switch>
        "data": {
<#if (request.notificationData.data)??>
  <#list request.notificationData.getData()?keys as key>
        "${key}": "${(request.notificationData.getData()[key]!"")?json_string}"<#sep>,
  </#list>
</#if>
        }
    }
</#compress>
}