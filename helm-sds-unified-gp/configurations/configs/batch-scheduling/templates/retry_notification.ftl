{
<#compress>
<#list request.requestObjectsList as notification>
    "notification": {
        "eventTag": "GENERIC_ALERT",
<#switch notification["notificationmanager.notificationMessage.notificationType"]>
  <#case "SMS">
        "smsTemplateName": "${notification["notificationmanager.notificationMessage.templateName"]}",
        "smsTemplateMessage": "${(notification["notificationmanager.notificationMessage.templateMessage"]!"")?json_string}",
        "msisdn": "${notification["notificationmanager.notificationMessage.recipientId"]}",
    <#break>
  <#case "EMAIL">
        "emailTemplateName": "${notification["notificationmanager.notificationMessage.templateName"]}",
        "emailTemplateMessage": "${(notification["notificationmanager.notificationMessage.templateMessage"]!"")?json_string}",
        "email": "${notification["notificationmanager.notificationMessage.recipientId"]}",
    <#break>
</#switch>
        "data": {
<#list notification?keys?filter(k -> k?starts_with("notificationmanager.notificationMessage.data.")) as key>
            "${key?remove_beginning("notificationmanager.notificationMessage.data.")}": "${(notification[key]!"")?json_string}"<#sep>,
</#list>
        }
    }
</#list>
</#compress>
}