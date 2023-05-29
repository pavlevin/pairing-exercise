package io.billie.invoices.service

import io.billie.invoices.model.InvoiceStatus
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class UpdateStatusesForSentInvoicesJob : Job {

    private val log : Logger = LoggerFactory.getLogger(this::class.java)
    @Throws(JobExecutionException::class)
    override fun execute(ctx: JobExecutionContext) {
        log.info("Updating invoice status job triggered")
        val invoiceSender = ctx.jobDetail.jobDataMap["invoiceSender"] as InvoiceSender
        if (invoiceSender.sentInvoicesWithOldStatus.isNotEmpty()) {
            invoiceSender.sentInvoicesWithOldStatus.forEach { invoiceSender.updateInvoiceStatus(it) }
            log.info("${invoiceSender.sentInvoicesWithOldStatus.size} invoices were updated to status ${InvoiceStatus.SENT.name}")
        } else {
            log.info("Nothing to update")
        }
    }
}