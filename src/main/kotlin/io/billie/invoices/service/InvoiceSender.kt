package io.billie.invoices.service

import io.billie.invoices.data.InvoicesRepository
import io.billie.invoices.model.InvoiceStatus
import io.billie.invoices.model.InvoicesResponse
import io.billie.utils.BillieQuartzScheduler
import io.billie.utils.Retrier
import org.apache.commons.mail.HtmlEmail
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.impl.triggers.CronTriggerImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct


@Component
class InvoiceSender(
    private val invoicesRepo: InvoicesRepository,
    private val scheduler: BillieQuartzScheduler,
    @Value("\${invoices.notification.mailingEnabled}") val emailingEnabled: Boolean
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    val sentInvoicesWithOldStatus: ConcurrentHashMap.KeySetView<UUID, Boolean> = ConcurrentHashMap.newKeySet()

    @PostConstruct
    fun init() {
        val jobKey = JobKey.jobKey("UpdateStatusesForSentInvoicesJob")
        val job = JobBuilder.newJob(UpdateStatusesForSentInvoicesJob::class.java)
            .withIdentity(jobKey)
            .setJobData(JobDataMap(mapOf("invoiceSender" to this)))
            .build()
        val trigger = CronTriggerImpl()
        trigger.cronExpression = "0 0/5 * ? * *"
        trigger.name = "UpdateStatusesForSentInvoicesTrigger"
        scheduler.scheduleJob(job, trigger)
    }

    fun sendInvoiceAndUpdateStatus(invoice: InvoicesResponse): UUID {
        invoicesRepo.updateInvoiceStatus(invoice.id, InvoiceStatus.READY_FOR_SENDING)
        if (!sentInvoicesWithOldStatus.contains(invoice.id)) {
            if (invoiceIsInCorrectState(invoice)) {
                Retrier.retry(
                    "Failed to send invoice <${invoice.id}> to buyer <${invoice.buyer.name}>",
                    "Invoice <${invoice.id}> was not sent to buyer <${invoice.buyer.name}>"
                ) {
                    sendInvoiceToBuyer(invoice)
                    log.info("Invoice <${invoice.id}> sent to buyer <${invoice.buyer.name}> successfully")
                }
                Retrier.retry(
                    "Failed to update states of sent invoice <${invoice.id}>",
                    "Invoice <${invoice.id}> was not updated in database"
                ) {
                    updateInvoiceStatus(invoice.id)
                    log.info("Invoice's <${invoice.id}> status was updated successfully")
                }
            }
        } else {
            val msg = "Invoice <${invoice.id}> is currently processed for sending to buyer or was sent earlier"
            log.warn(msg)
            throw IllegalStateException(msg)
        }
        return invoice.id
    }

    private fun invoiceIsInCorrectState(invoice: InvoicesResponse): Boolean {
        return (invoice.status == InvoiceStatus.NEW || invoice.status == InvoiceStatus.READY_FOR_SENDING)
    }

    internal fun sendInvoiceToBuyer(invoice: InvoicesResponse) {
        if (emailingEnabled) {
            HtmlEmail()
                .setFrom("invoice-for-clients-no-reply@billie.com")
                .addTo(invoice.buyer.contactDetails.email)
                .setSubject("Invoice ${invoice.invoiceId} issued. ")
                .setMsg(
                    """
                Dear user. 
                
                Goods were shipped to you and we are waiting for the payment for invoice ${invoice.invoiceId} for 
                the amount ${invoice.amount} ${invoice.ccy.name}.
            
                Please do not reply to this email.
                
                In case of any questions please contact client-support@billie.com
                """
                        .trimIndent()
                )
                .send()
        }
        sentInvoicesWithOldStatus.add(invoice.id)
    }

    fun updateInvoiceStatus(invoiceId: UUID) {
        invoicesRepo.updateInvoiceStatus(invoiceId, InvoiceStatus.SENT)
        sentInvoicesWithOldStatus.remove(invoiceId)
    }
}