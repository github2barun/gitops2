"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getProductCategory()??>

   "pms.categoryId": "${response.getProductCategory().getId()}",
   "pms.categoryParentId": "${response.getProductCategory().getParentId()!""}",
   "pms.categoryPath": "${response.getProductCategory().getPath()!""}",
   "pms.categoryName": "${response.getProductCategory().getName()}",
   "pms.categoryDescription": "${response.getProductCategory().getDescription()!""}",
   "pms.workflowId": "${response.getProductCategory().getWorkflowId()!""}",
   
   <#if response.getProductCategory().getAvailableFrom()??>
    "pms.categoryAvailableFrom": "${response.getProductCategory().getAvailableFrom()}",
   </#if>
   <#if response.getProductCategory().getAvailableUntil()??>
   "pms.categoryAvailableUntil": "${response.getProductCategory().getAvailableUntil()}",
   </#if>
   
   "pms.categoryStatus": "${response.getProductCategory().getStatus()}"
<#else>

</#if>