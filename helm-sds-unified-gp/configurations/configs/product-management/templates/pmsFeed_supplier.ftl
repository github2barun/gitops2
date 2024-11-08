"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getSupplier()??>

"pms.supplierId": "${response.getSupplier().getId()}",
"pms.supplierCode": "${response.getSupplier().getCode()}",
"pms.supplierName": "${response.getSupplier().getName()}",
"pms.supplierDescription": "${response.getSupplier().getDescription()}"

<#else>

</#if>