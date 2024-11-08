<#setting number_format="0" />
"oms.resultcode":${(response.resultCode)!'N/A'},
<#setting number_format="" />
"oms.resultMessage":"${(response.resultMessage)!'N/A'}",
"oms.orderId":"${(response.orderId)!'N/A'}",
"oms.orderType":"${(response.orderType)!'N/A'}",
"oms.paymentMode":"${(response.paymentMode)!'N/A'}",
"oms.paymentAgreement":"${(response.paymentAgreement)!'N/A'}",
"oms.orderStatus":"${(response.orderStatus)!'N/A'}",
<#if response.items??>
"oms.items":[<#list response.items as item>
{
    "productCode":"${(item.productCode)!'N/A'}",
    "categoryPath":"${(item.categoryPath)!'N/A'}",
    "productSku":"${(item.productSku)!'N/A'}",
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
    "data":{<#list item.attributes?keys as key>
        "${key!'N/A'}":"${item.attributes[key]!'N/A'}"
        <#sep>,
    </#list>}
}
    <#sep>,
</#list>]
</#if>
"oms.additionalFields":{<#list response.orderFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
"oms.tripId":"${(response.tripId)!'N/A'}",
"oms.linkedOrderIds":[<#list response.linkedOrderIds as orderId>
    "${orderId}"
    <#sep>,
</#list>],
"oms.returnType":"${(response.returnType)!'N/A'}",
"oms.returnReason":"${(response.returnReason)!'N/A'}",
<#if response.invoices??>
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
</#if>
<#if response.seller??>
"oms.seller.id":"${(response.seller.id)!'N/A'}",
"oms.seller.additionalFields":{<#list response.seller.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
"oms.seller.omsFields":{<#list response.seller.omsFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
<#if response.buyer??>
"oms.buyer.id":"${(response.buyer.id)!'N/A'}",
"oms.buyer.additionalFields":{<#list response.buyer.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
"oms.buyer.omsFields":{<#list response.buyer.omsFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
<#if response.sender??>
"oms.sender.id":"${(response.sender.id)!'N/A'}",
"oms.sender.additionalFields":{<#list response.sender.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
"oms.sender.omsFields":{<#list response.sender.omsFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
"oms.receivers":[<#list response.receivers as receiver>
{
    <#if receiver??>
    "id":"${(receiver.id)!'N/A'}",
    "additionalFields":{<#list receiver.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    "omsFields":{<#list receiver.omsFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
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
    "oms.pickup.additionalFields":{<#list deliveryPath.pickupPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    "oms.pickup.omsFields":{<#list deliveryPath.pickupPoint.omsFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    <#if deliveryPath.dropPoint??>
    "oms.drop.id":"${(deliveryPath.dropPoint.id)!'N/A'}",
    "oms.drop.additionalFields":{<#list deliveryPath.dropPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    "oms.drop.omsFields":{<#list deliveryPath.dropPoint.omsFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    <#if deliveryPath.reconciliationPoint??>
    "oms.reconciliation.id":"${(deliveryPath.reconciliationPoint.id)!'N/A'}",
    "oms.reconciliation.additionalFields":{<#list deliveryPath.reconciliationPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    "oms.reconciliation.omsFields":{<#list deliveryPath.reconciliationPoint.omsFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    "oms.delivery.agentType":"${(deliveryPath.agentType)!'N/A'}"
}
    <#sep>,
</#list>]