package io.billie.invoices.model

enum class InvoiceStatus {
    NEW,
    READY_FOR_SENDING,
    SENT,
    SETTLED,
    FAILED, // e.g. set up for organisation is not correct. Could be resent manually by ops
    CANCELLED // e.g. if shipment was not delivered and merchant got goods back

}
