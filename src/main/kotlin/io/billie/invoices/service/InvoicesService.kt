package io.billie.invoices.service

import io.billie.invoices.data.InvoicesRepository
import io.billie.invoices.model.InvoiceStatus
import io.billie.invoices.model.InvoicesRequest
import io.billie.invoices.model.InvoicesResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class InvoicesService(
    private val invoicesRepo: InvoicesRepository,
    private val invoiceSender: InvoiceSender
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun createInvoice(request: InvoicesRequest): UUID {
        val fieldNameAndIsItNull = listOf(
            "buyer" to (request.buyerId == null),
            "currency" to (request.ccy == null),
            "amount" to (request.amount == null)
        )
        if (fieldNameAndIsItNull.any { it.second }) {
            val msg = "Request for invoice creation is not valid. Missing field(s): ${
                fieldNameAndIsItNull.filter { it.second }
                    .joinToString(separator = ",", prefix = "<", postfix = ">") { it.first }
            }"
            log.error(msg)
            throw IllegalArgumentException(msg)
        }
        return invoicesRepo.createInvoice(request)
    }

    fun orderIsShipped(request: InvoicesRequest) : UUID {
        val invoice = getInvoiceByIdAndSupplier(request)
        return invoiceSender.sendInvoiceAndUpdateStatus(invoice)
    }

    fun getInvoiceByIdAndSupplier(request: InvoicesRequest): InvoicesResponse = invoicesRepo.getInvoiceByIdAndSupplier(request)

}


