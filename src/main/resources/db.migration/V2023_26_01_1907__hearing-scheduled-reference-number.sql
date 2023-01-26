CREATE SEQUENCE hearing_scheduled_reference_number_seq MAXVALUE 999999 NO CYCLE;

CREATE FUNCTION next_hearing_scheduled_reference_number() RETURNS TEXT AS $$
SELECT
  regexp_replace(
    to_char(
      nextval('hearing_scheduled_reference_number_seq'),
      'FM000000'),
    '(\d{3})(\d{3})', '\1HN\2')
$$ LANGUAGE SQL;
