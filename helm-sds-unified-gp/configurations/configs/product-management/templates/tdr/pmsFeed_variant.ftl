"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getProductVariant()??>
	<#assign variant=response.getProductVariant() />

     "pms.variantId": "${variant.getVariantId()!""}",
     "pms.productSKU": "${variant.getProductSKU()!""}",
     "pms.supplierReference": "${variant.getSupplierReference()!""}",
     "pms.status": "${variant.getStatus()!""}",
     "pms.EANCode": "${variant.getEaNCode()!""}",
     "pms.variantImageURL": "${variant.getImageUrl()!""}",
   
   	 <#if variant.getAvailableFrom()??>
       "pms.variantAvailableFrom": "${variant.getAvailableFrom()}",
     </#if>
     <#if variant.getAvailableUntil()??>
       "pms.variantAvailableUntil": "${variant.getAvailableUntil()}",
     </#if>
             
     <#if variant.getUnitPrice()??>
     <#assign unitPrice=variant.getUnitPrice() />
	 	"pms.variantPrice": "${unitPrice.getPrice()!""}",
	 	"pms.variantVariablePrice": "${unitPrice.isVariablePrice()?c}",
	 	"pms.variantPriceCurrency": "${unitPrice.getCurrency()}",
     <#else>
     </#if>

     <#if variant.getWeight()??>
     <#assign weight=variant.getWeight() />
	 	"pms.variantWeightUnit": "${weight.getUnit()!""}",
	 	"pms.variantWeightValue": "${weight.getValue()!""}",
     <#else>
     </#if>

     <#if variant.getVolume()??>
     <#assign volume=variant.getVolume() />
	 	"pms.variantVolumeUnit": "${volume.getUnit()!""}",
	 	"pms.variantVolumeValue": "${volume.getValue()!""}",
     <#else>
     </#if>

     <#if variant.getWarrantyPeriod()??>
     <#assign warranty=variant.getWarrantyPeriod() />
	 	"pms.variantWarrantyUnit": "${warranty.getUnit()!""}",
	 	"pms.variantWarrantyValue": "${warranty.getValue()!""}",
     <#else>
     </#if>

     <#if variant.getUnitOfMeasure()??>
     <#assign uom=variant.getUnitOfMeasure() />
	 	"pms.variantUOMUnit": "${uom.getUnit()!""}",
	 	"pms.variantUOMQuantity": "${uom.getQuantity()!""}",
     <#else>
     </#if>
     
   	 <#if variant.getCreatedAt()??>
       "pms.variantCreatedAt": "${variant.getCreatedAt()?string('dd.MM.yyyy')}",
     </#if>
     <#if variant.getUpdatedAt()??>
       "pms.variantUpdatedAt": "${variant.getUpdatedAt()?string('dd.MM.yyyy')}"
     </#if>

<#else>

</#if>