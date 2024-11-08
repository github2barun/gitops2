"oms.resultCode":${(response.resultCode)!'N/A'},
"oms.resultMessage":"${(response.resultMessage)!'N/A'}",
"oms.headers.clientComment":"${(response.clientComment)!'N/A'}",
"oms.headers.clientReference":"${(response.clientReference)!'N/A'}",
"oms.orderId":"${(response.orderId)!'N/A'}",
"oms.orderType":"${(response.orderType)!'N/A'}",
"oms.paymentMode":"${(response.paymentMode)!'N/A'}",
"oms.paymentAgreement":"${(response.paymentAgreement)!'N/A'}",
"oms.orderStatus":"${(response.orderStatus)!'N/A'}",
"oms.items":[<#if response.items?? >
<#list response.items as item>
{
"productCode":"${(item.productCode)!'N/A'}",
"categoryPath":"${(item.categoryPath)!'N/A'}",
"productSku":"${(item.productSku)!'N/A'}",
"productType":"${(item.productType)!'N/A'}",
"productName":"${(item.productName)!'N/A'}",
"productDescription":"${(item.productDescription)!'N/A'}",
"quantity":"${(item.quantity)!'N/A'}",
"reserveType":"${(item.reserveType)!'N/A'}",
"serials":[<#list item.serials as serialNo>
"${serialNo}"
<#sep>,
</#list>],
"batchIds":[<#list item.batchIds as batchId>
"${batchId}"
<#sep>,
</#list>],
"ranges":[<#list item.ranges as range>
{
"batchId":"${(range.batchId)!'N/A'}",
"startSerial":"${(range.startSerial)!'N/A'}",
"endSerial":"${(range.endSerial)!'N/A'}"
}
<#sep>,
</#list>],
"data":{<#if item.attributes?? >
<#list item.attributes?keys as key>
"${key!'N/A'}":"${item.attributes[key]!'N/A'}"
<#sep>,
</#list>
<#else>
</#if>
}
}
<#sep>,
</#list>
<#else>
</#if>
],
"oms.additionalFields":{<#list response.orderFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.tripId":"${(response.tripId)!'N/A'}",
"oms.linkedOrderIds":[<#list response.linkedOrderIds as orderId>
"${orderId}"
<#sep>,
</#list>],
"oms.invoices":[<#list response.invoices as invoice>{
"invoice.id":"${(invoice.invoiceId)!'N/A'}",
"invoice.status":"${(invoice.status)!'N/A'}",
"invoice.totalUnitPrice":{
<#if invoice.totalUnitPrice?? >
"amount":"${(invoice.totalUnitPrice.amount)!'N/A'}",
"currency":"${(invoice.totalUnitPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalOfferPrice":{
<#if invoice.totalOfferPrice?? >
"amount":"${(invoice.totalOfferPrice.amount)!'N/A'}",
"currency":"${(invoice.totalOfferPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalCalculatedTax":{
<#if invoice.totalCalculatedTax?? >
"amount":"${(invoice.totalCalculatedTax.amount)!'N/A'}",
"currency":"${(invoice.totalCalculatedTax.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalDiscount":{
<#if invoice.totalDiscount?? >
"amount":"${(invoice.totalDiscount.amount)!'N/A'}",
"currency":"${(invoice.totalDiscount.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalRetailPrice":{
<#if invoice.totalRetailPrice?? >
"amount":"${(invoice.totalRetailPrice.amount)!'N/A'}",
"currency":"${(invoice.totalRetailPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalSenderCommission":{
<#if invoice.totalSenderCommission?? >
"amount":"${(invoice.totalSenderCommission.amount)!'N/A'}",
"currency":"${(invoice.totalSenderCommission.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.totalReceiverCommission":{
<#if invoice.totalReceiverCommission?? >
"amount":"${(invoice.totalReceiverCommission.amount)!'N/A'}",
"currency":"${(invoice.totalReceiverCommission.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"invoice.invoiceEntries":[<#list invoice.invoiceEntryList as invoiceEntry>{
<#if invoiceEntry.product?? >
"productId":"${(invoiceEntry.product.productId)!'N/A'}",
"productCode":"${(invoiceEntry.product.productCode)!'N/A'}",
"productName":"${(invoiceEntry.product.productName)!'N/A'}",
"productSKU":"${(invoiceEntry.product.productSKU)!'N/A'}",
"productDescription":"${(invoiceEntry.product.productDescription)!'N/A'}",
"categoryPath":"${(invoiceEntry.product.categoryPath)!'N/A'}",
"productType":"${(invoiceEntry.product.productType)!'N/A'}",
"taxes":[<#list invoiceEntry.product.taxDetails as tax>{
"taxId":"${(tax.id)!'N/A'}",
"taxType":"${(tax.taxType)!'N/A'}",
"percentValue":"${(tax.percentValue)!'N/A'}",
"fixedValue":"${(tax.fixedValue)!'N/A'}"
}
<#sep>,
</#list>],
<#else>
"productId":"${'N/A'}",
"productCode":"${'N/A'}",
"productName":"${'N/A'}",
"productSKU":"${'N/A'}",
"productDescription":"${'N/A'}",
"categoryPath":"${'N/A'}",
"productType":"${'N/A'}",
"taxes":[],
</#if>
"quantity":<#if invoiceEntry.uom?? >
"${(invoiceEntry.uom.quantity)!'N/A'}",
<#else>
"${'N/A'}",
</#if>
"totalUnitPrice":{
<#if invoiceEntry.totalUnitPrice?? >
"amount":"${(invoiceEntry.totalUnitPrice.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalUnitPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalOfferPrice":{
<#if invoiceEntry.totalOfferPrice?? >
"amount":"${(invoiceEntry.totalOfferPrice.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalOfferPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalCalculatedTax":{
<#if invoiceEntry.totalCalculatedTax?? >
"amount":"${(invoiceEntry.totalCalculatedTax.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalCalculatedTax.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalDiscount":{
<#if invoiceEntry.totalDiscount?? >
"amount":"${(invoiceEntry.totalDiscount.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalDiscount.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalRetailPrice":{
<#if invoiceEntry.totalRetailPrice?? >
"amount":"${(invoiceEntry.totalRetailPrice.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalRetailPrice.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalSenderCommission":{
<#if invoiceEntry.totalSenderCommission?? >
"amount":"${(invoiceEntry.totalSenderCommission.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalSenderCommission.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"totalReceiverCommission":{
<#if invoiceEntry.totalReceiverCommission?? >
"amount":"${(invoiceEntry.totalReceiverCommission.amount)!'N/A'}",
"currency":"${(invoiceEntry.totalReceiverCommission.currency)!'N/A'}"
<#else>
"amount":"${'N/A'}",
"currency":"${'N/A'}"
</#if>
},
"data":{<#list invoiceEntry.attributes?keys as key>
"${key!'N/A'}":"${invoiceEntry.attributes[key]!'N/A'}"
<#sep>,
</#list>}
}<#sep>,
</#list>]
}<#sep>,
</#list>],
"oms.returnType":"${(response.returnType)!'N/A'}",
"oms.returnReason":"${(response.returnReason)!'N/A'}",
<#if response.seller??>
"oms.seller.id":"${(response.seller.id)!'N/A'}",
"oms.seller.name":"${(response.seller.name)!'N/A'}",
"oms.seller.email":"${(response.seller.email)!'N/A'}",
"oms.seller.msisdn":"${(response.seller.msisdn)!'N/A'}",
"oms.seller.accountType":"${(response.seller.accountType)!'N/A'}",
"oms.seller.sellerType":"${(response.seller.typeId)!'N/A'}",
"oms.seller.street":
<#if response.seller.address??>
"${(response.seller.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.zip":
<#if response.seller.address??>
"${(response.seller.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.city":
<#if response.seller.address??>
"${(response.seller.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.country":
<#if response.seller.address??>
"${(response.seller.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.fax":
<#if response.seller.address??>
"${(response.seller.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.homepage":
<#if response.seller.address??>
"${(response.seller.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.seller.additionalFields":{<#list response.seller.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.seller.omsFields":{<#list response.seller.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
<#if response.buyer??>
"oms.buyer.id":"${(response.buyer.id)!'N/A'}",
"oms.buyer.name":"${(response.buyer.name)!'N/A'}",
"oms.buyer.email":"${(response.buyer.email)!'N/A'}",
"oms.buyer.msisdn":"${(response.buyer.msisdn)!'N/A'}",
"oms.buyer.buyerType":"${(response.buyer.typeId)!'N/A'}",
"oms.buyer.accountType":"${(response.buyer.accountType)!'N/A'}",
"oms.buyer.street":
<#if response.buyer.address??>
"${(response.buyer.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.zip":
<#if response.buyer.address??>
"${(response.buyer.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.city":
<#if response.buyer.address??>
"${(response.buyer.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.country":
<#if response.buyer.address??>
"${(response.buyer.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.fax":
<#if response.buyer.address??>
"${(response.buyer.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.homepage":
<#if response.buyer.address??>
"${(response.buyer.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.buyer.additionalFields":{<#list response.buyer.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.buyer.omsFields":{<#list response.buyer.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
<#if response.sender??>
"oms.sender.id":"${(response.sender.id)!'N/A'}",
"oms.sender.name":"${(response.sender.name)!'N/A'}",
"oms.sender.email":"${(response.sender.email)!'N/A'}",
"oms.sender.msisdn":"${(response.sender.msisdn)!'N/A'}",
"oms.sender.senderType":"${(response.sender.typeId)!'N/A'}",
"oms.sender.accountType":"${(response.sender.accountType)!'N/A'}",
"oms.sender.street":
<#if response.sender.address??>
"${(response.sender.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.zip":
<#if response.sender.address??>
"${(response.sender.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.city":
<#if response.sender.address??>
"${(response.sender.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.country":
<#if response.sender.address??>
"${(response.sender.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.fax":
<#if response.sender.address??>
"${(response.sender.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.homepage":
<#if response.sender.address??>
"${(response.sender.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.sender.additionalFields":{<#list response.sender.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.sender.omsFields":{<#list response.sender.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
"oms.receivers":[<#list response.receivers as receiver>
{
<#if receiver??>
"id":"${(receiver.id)!'N/A'}",
"name":"${(receiver.name)!'N/A'}",
"email":"${(receiver.email)!'N/A'}",
"msisdn":"${(receiver.msisdn)!'N/A'}",
"receiverType":"${(receiver.typeId)!'N/A'}",
"accountType":"${(receiver.accountType)!'N/A'}",
"street":
<#if receiver.address??>
"${(receiver.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"zip":
<#if receiver.address??>
"${(receiver.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"city":
<#if receiver.address??>
"${(receiver.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"country":
<#if receiver.address??>
"${(receiver.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"fax":
<#if receiver.address??>
"${(receiver.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"homepage":
<#if receiver.address??>
"${(receiver.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"additionalFields":{<#list receiver.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"omsFields":{<#list receiver.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>}
</#if>
}
<#sep>,
</#list>],
"oms.deliveryPaths":[<#list response.deliveryPaths as deliveryPath>
{
<#if deliveryPath.pickupPoint??>
"oms.pickup.id":"${(deliveryPath.pickupPoint.id)!'N/A'}",
"oms.pickup.name":"${(deliveryPath.pickupPoint.name)!'N/A'}",
"oms.pickup.email":"${(deliveryPath.pickupPoint.email)!'N/A'}",
"oms.pickup.msisdn":"${(deliveryPath.pickupPoint.msisdn)!'N/A'}",
"oms.pickup.street":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.zip":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.city":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.country":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.fax":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.homepage":
<#if deliveryPath.pickupPoint.address??>
"${(deliveryPath.pickupPoint.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.pickup.additionalFields":{<#list deliveryPath.pickupPoint.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.pickup.omsFields":{<#list deliveryPath.pickupPoint.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
<#if deliveryPath.dropPoint??>
"oms.drop.id":"${(deliveryPath.dropPoint.id)!'N/A'}",
"oms.drop.name":"${(deliveryPath.dropPoint.name)!'N/A'}",
"oms.drop.email":"${(deliveryPath.dropPoint.email)!'N/A'}",
"oms.drop.msisdn":"${(deliveryPath.dropPoint.msisdn)!'N/A'}",
"oms.drop.street":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.zip":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.city":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.country":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.fax":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.homepage":
<#if deliveryPath.dropPoint.address??>
"${(deliveryPath.dropPoint.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.drop.additionalFields":{<#list deliveryPath.dropPoint.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.drop.omsFields":{<#list deliveryPath.dropPoint.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
<#if deliveryPath.reconciliationPoint??>
"oms.reconciliation.id":"${(deliveryPath.reconciliationPoint.id)!'N/A'}",
"oms.reconciliation.name":"${(deliveryPath.reconciliationPoint.name)!'N/A'}",
"oms.reconciliation.email":"${(deliveryPath.reconciliationPoint.email)!'N/A'}",
"oms.reconciliation.msisdn":"${(deliveryPath.reconciliationPoint.msisdn)!'N/A'}",
"oms.reconciliation.street":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.street)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.zip":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.zip)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.city":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.city)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.country":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.country)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.fax":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.fax)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.homepage":
<#if deliveryPath.reconciliationPoint.address??>
"${(deliveryPath.reconciliationPoint.address.homepage)!'N/A'}"
<#else>
"${'N/A'}"
</#if>,
"oms.reconciliation.additionalFields":{<#list deliveryPath.reconciliationPoint.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
<#sep>,
</#list>},
"oms.reconciliation.omsFields":{<#list deliveryPath.reconciliationPoint.omsFields as field>
<#if field.name?has_content>
"${(field.name)!'N/A'}":"${field.value!'N/A'}"
</#if>
<#sep>,
</#list>},
</#if>
"oms.delivery.agentType":"${(deliveryPath.agentType)!'N/A'}"
}
<#sep>,
</#list>]