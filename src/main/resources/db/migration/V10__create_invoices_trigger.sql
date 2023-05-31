create or REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER INVOICES_TGR
   BEFORE INSERT OR UPDATE
   ON organisations_schema.invoices
   FOR EACH row
   execute procedure update_updated_at();
;

