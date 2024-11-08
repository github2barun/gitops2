"billingsettlement.ersReference": "${response.getErsReference()!""}",
"billingsettlement.resultcode": "${response.getResultCode()!400}",
"billingsettlement.resultMessage": "${response.getResultMessage()!""}"

<#if (response.invoice)??>,
 "billingsettlement.invoice.invoiceId": "${response.getInvoice().getInvoiceId()!""}",
 "billingsettlement.invoice.vendor": "${response.getInvoice().getVendor()!""}",
 "billingsettlement.invoice.totalTrips": "${response.getInvoice().getTotalTrips()!""}",
 "billingsettlement.invoice.fromDate": "${response.getInvoice().getFromDate()!""}",
 "billingsettlement.invoice.toDate": "${response.getInvoice().getToDate()!""}",
 "billingsettlement.invoice.tripFare": "${response.getInvoice().getTripFare()!""}",
 "billingsettlement.invoice.paid": "${response.getInvoice().getPaid()!""}",
 "billingsettlement.invoice.status": "${response.getInvoice().getStatus()!""}",
 "billingsettlement.invoice.createdDate": "${response.getInvoice().getCreatedDate()!""}",
 "billingsettlement.invoice.lastModifiedDate": "${response.getInvoice().getLastModifiedDate()!""}",
 "billingsettlement.invoice.totalOrdersProcessed": "${response.getInvoice().getTotalOrdersProcessed()!""}",
 "billingsettlement.invoice.totalTripCapacity": "${response.getInvoice().getTotalTripCapacity()!""}"
</#if> 

