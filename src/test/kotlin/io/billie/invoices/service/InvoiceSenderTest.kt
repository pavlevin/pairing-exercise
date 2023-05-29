package io.billie.invoices.service

import io.billie.invoices.data.InvoicesRepository
import io.billie.invoices.model.InvoiceStatus
import io.billie.invoices.model.InvoicesResponse
import io.billie.utils.BillieQuartzScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class InvoiceSenderTest {

    private val repository = mockk<InvoicesRepository>(relaxed = true)
    private val scheduler = mockk<BillieQuartzScheduler>(relaxed = true)
    private val sender: InvoiceSender = spyk(InvoiceSender(repository, scheduler, false))


    @Test
    fun `Invoice successfully sent`() {
        val invoicesResponse = mockk<InvoicesResponse>(relaxed = true)
        every { invoicesResponse.status } returns InvoiceStatus.NEW
        every { invoicesResponse.id } returns UUID.randomUUID()
        sender.sendInvoiceAndUpdateStatus(invoicesResponse)
        verify(exactly = 1) { sender.sendInvoiceToBuyer(invoicesResponse) }
        verify(exactly = 1) { repository.updateInvoiceStatus(invoicesResponse.id, InvoiceStatus.READY_FOR_SENDING) }
        verify(exactly = 1) { repository.updateInvoiceStatus(invoicesResponse.id, InvoiceStatus.SENT) }

    }
    @Test
    fun `Invoice is remains in status READY_FOR_SENT if it was not sent to the buyer`() {
        val invoicesResponse = mockk<InvoicesResponse>(relaxed = true)
        every { invoicesResponse.status } returns InvoiceStatus.NEW
        every { invoicesResponse.id } returns UUID.randomUUID()
        every { sender.emailingEnabled } returns true
        assertThrows<RuntimeException>("Invoice <${invoicesResponse.id}> was not sent to buyer <>") {
            sender.sendInvoiceAndUpdateStatus(invoicesResponse)
        }
        verify(exactly = 5) { sender.sendInvoiceToBuyer(invoicesResponse) }
        verify(exactly = 1) { repository.updateInvoiceStatus(any(), eq(InvoiceStatus.READY_FOR_SENDING)) }
        verify(exactly = 0) { repository.updateInvoiceStatus(any(), eq(InvoiceStatus.SENT)) }
    }


}