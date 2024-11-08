<#compress>
"notificationmanager.ersReference": "${response.getErsReference()!""}",
"notificationmanager.resultcode": "${response.getResultCode()!400}",
"notificationmanager.resultMessage": "${response.getResultMessage()!""}"

<#if (response.notificationMessage)??>,
 "notificationmanager.notificationMessage.recipientId": "${response.getNotificationMessage().getRecipientId()!""}",
 "notificationmanager.notificationMessage.senderId": "${response.getNotificationMessage().getSenderId()!""}",
 "notificationmanager.notificationMessage.message": "${response.getNotificationMessage().getMessage()!""}",
 "notificationmanager.notificationMessage.notificationType": "${response.getNotificationMessage().getNotificationType()!""}",
 "notificationmanager.notificationMessage.referenceEventId": "${response.getNotificationMessage().getReferenceEventId()!""}",
 "notificationmanager.notificationMessage.deliveryTime": "${response.getNotificationMessage().getDeliveryTime()!""}",
 "notificationmanager.notificationMessage.languageCode": "${response.getNotificationMessage().getLanguageCode()!""}",
 "notificationmanager.notificationMessage.templateName": "${response.getNotificationMessage().getTemplateName()!""}",
 "notificationmanager.notificationMessage.templateMessage": "${(response.getNotificationMessage().getTemplateMessage()!"")?json_string}",
 "notificationmanager.notificationMessage.status": "${response.getNotificationMessage().getStatus()!""}"
 
   <#if response.getNotificationMessage().getData()??>,
   <#list response.getNotificationMessage().getData()?keys as data>
        "notificationmanager.notificationMessage.data.${data}": "${response.getNotificationMessage().getData()[data]}"
    	<#if data_has_next>, </#if>
   </#list>
   </#if>
   
</#if>
</#compress>

