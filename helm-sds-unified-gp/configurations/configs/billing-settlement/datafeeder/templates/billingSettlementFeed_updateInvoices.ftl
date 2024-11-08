"billingsettlement.ersReference": "${response.getErsReference()!""}",
"billingsettlement.resultcode": "${response.getResultCode()!400}",
"billingsettlement.resultMessage": "${response.getResultMessage()!""}"

<#if (response.invoice)??>,
"billingsettlement.invoices": [
   <#list response.getInvoices() as invoice>
{
"invoiceId": "${invoice.getInvoiceId()!""}",
"vendor": "${invoice.getVendor()!""}",
"totalTrips": "${invoice.getTotalTrips()!""}",
"fromDate": "${invoice.getFromDate()!""}",
"toDate": "${invoice.getToDate()!""}",
"tripFare": "${invoice.getTripFare()!""}",
"paid": "${invoice.getPaid()!""}",
"status": "${invoice.getStatus()!""}",
"createdDate": "${invoice.getCreatedDate()!""}",
"lastModifiedDate": "${invoice.getLastModifiedDate()!""}",
"totalOrdersProcessed": "${invoice.getTotalOrdersProcessed()!""}",
"totalTripCapacity": "${invoice.getTotalTripCapacity()!""}"
}
   <#if invoice_has_next>, </#if>
   </#list>
]
</#if>
<#if response.getInvoicesNotFound()??>,
"billingsettlement.invoicesNotFound": [
   <#list response.getInvoicesNotFound() as invoiceId>
{
"invoiceId": "${invoiceId!""}"
}
   <#if invoiceId_has_next>, </#if>
   </#list>
]
</#if>
<#if response.getInvoicesAlreadyPaid()??>,
"billingsettlement.invoicesAlreadyPaid": [
   <#list response.getInvoicesAlreadyPaid() as invoiceId>
{
"invoiceId": "${invoiceId!""}"
}
   <#if invoiceId_has_next>, </#if>
   </#list>
]
</#if>