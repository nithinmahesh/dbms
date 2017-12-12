-- run time on S0 DB: 1 sec
-- result row count: 4 rows
select distinct C.name as carrier from FLIGHTS F, CARRIERS C
where F.origin_city = 'Seattle WA' and F.dest_city = 'San Francisco CA'
and F.carrier_id = C.cid

-- Partial output:
--Alaska Airlines Inc.
--SkyWest Airlines Inc.
--United Air Lines Inc.
--Virgin America