ALTER TABLE charge_templates
    DROP CONSTRAINT ck_charge_templates_systematic;

ALTER TABLE charge_templates
    ADD CONSTRAINT ck_charge_templates_systematic CHECK (
        (
            type = 'SYSTEMATIC'
            AND periodicity = 'MONTHLY'
            AND (
                (poll_enabled = FALSE AND day_of_month IS NOT NULL)
                OR (poll_enabled = TRUE AND day_of_month IS NULL)
            )
        )
        OR
        (type = 'ONE_TIME' AND periodicity = 'ONE_TIME')
    );
