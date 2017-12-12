-- run time on S0 DB: 11 sec
-- result row count: 256 rows
select distinct F2.dest_city as city from FLIGHTS F1, FLIGHTS F2
where F1.dest_city = F2.origin_city and F1.origin_city = 'Seattle WA' 
and F2.dest_city != 'Seattle WA' and F2.dest_city not in
(select distinct dest_city from FLIGHTS where origin_city = 'Seattle WA')

-- Partial output:
--Dothan AL
--Toledo OH
--Peoria IL
--Yuma AZ
--Bakersfield CA
--Daytona Beach FL
--Laramie WY
--North Bend/Coos Bay OR
--Erie PA
--Guam TT
--Columbus GA
--Wichita Falls TX
--Hartford CT
--Myrtle Beach SC
--Arcata/Eureka CA
--Kotzebue AK
--Medford OR
--Providence RI
--Green Bay WI
--Santa Maria CA