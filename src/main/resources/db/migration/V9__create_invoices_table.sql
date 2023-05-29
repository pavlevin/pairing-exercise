CREATE TABLE IF NOT EXISTS organisations_schema.invoices
(
    id              UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    invoice_id      VARCHAR(255),
    buyer_id        UUID NOT NULL,
    supplier_id     UUID NOT NULL,
    status          VARCHAR(255) NOT NULL,
    amount          NUMERIC(29, 2) NOT NULL,
    ccy             VARCHAR(3) NOT NULL,
    invoice_date    DATE        NOT NULL,
    payment_date    DATE,
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(255),
    UNIQUE (invoice_id, supplier_id)
);
