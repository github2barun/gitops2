"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getProduct()??>

   "pms.id": "${response.getProduct().getId()!""}",
   "pms.code": "${response.getProduct().getCode()!""}",
   "pms.name": "${response.getProduct().getName()!""}",
   "pms.description": "${response.getProduct().getDescription()!""}",
   "pms.categoryId": "${response.getProduct().getCategoryId()!""}",
   "pms.supplierId": "${response.getProduct().getSupplierId()!""}",
   "pms.workflowId": "${response.getProduct().getWorkflowId()!""}",
   "pms.productType": "${response.getProduct().getProductType()}",
   <#if response.getProduct().getAvailableFrom()??>
    "pms.availableFrom": "${response.getProduct().getAvailableFrom()}",
   </#if>
   <#if response.getProduct().getAvailableUntil()??>
   "pms.availableUntil": "${response.getProduct().getAvailableUntil()}"
   </#if>
<#else>

</#if>