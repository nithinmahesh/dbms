CREATE TABLE CARRIERS (cid varchar(7) PRIMARY KEY, name varchar(83));
CREATE TABLE MONTHS (mid int PRIMARY KEY, month varchar(9));
CREATE TABLE WEEKDAYS (did int PRIMARY KEY, day_of_week varchar(9));

CREATE TABLE FLIGHTS (fid int PRIMARY KEY, 
			year int, 
			month_id int FOREIGN KEY REFERENCES MONTHS(mid), 
			day_of_month int, 
			day_of_week_id int FOREIGN KEY REFERENCES WEEKDAYS(did), 
			carrier_id varchar(7) FOREIGN KEY REFERENCES CARRIERS(cid), 
			flight_num int,
			origin_city varchar(34), 
			origin_state varchar(47), 
			dest_city varchar(34), 
			dest_state varchar(46), 
			departure_delay float, 
			taxi_out float, 
			arrival_delay float,
			canceled int, 
			actual_time float, 
			distance float, 
			capacity int, 
			price float);

