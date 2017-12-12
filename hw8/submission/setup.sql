--CREATE TABLE FLIGHTS
-- (
--  fid int NOT NULL PRIMARY KEY,
--  year int,
--  month_id int,
--  day_of_month int,
--  day_of_week_id int,
--  carrier_id varchar(3),
--  flight_num int,
--  origin_city varchar(34),
--  origin_state varchar(47),
--  dest_city varchar(34),
--  dest_state varchar(46),
--  departure_delay double precision,
--  taxi_out double precision,
--  arrival_delay double precision,
--  canceled int,
--  actual_time double precision,
--  distance double precision,
--  capacity int,
--  price double precision
--)

DROP TABLE IF EXISTS ITINERARIES
DROP TABLE IF EXISTS RESERVATIONS
DROP TABLE IF EXISTS USERS

CREATE NONCLUSTERED INDEX nc_idx_flights_fid ON FLIGHTS 
(
	[fid] ASC
);

CREATE NONCLUSTERED INDEX nc_idx_flights_search ON FLIGHTS (
	day_of_month, origin_city, dest_city, canceled
);


/****** Object:  StoredProcedure [dbo].[sp_resetFlightCapacity]    Script Date: 12/3/2017 7:32:03 PM ******/
DROP PROCEDURE IF EXISTS [dbo].[sp_resetFlightCapacity]
GO

/****** Object:  StoredProcedure [dbo].[sp_resetFlightCapacity]    Script Date: 12/3/2017 7:32:04 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [dbo].[sp_resetFlightCapacity]
AS
BEGIN
	IF COL_LENGTH('dbo.FLIGHTS', 'booked_capacity') IS NOT NULL
	BEGIN
		ALTER TABLE FLIGHTS DROP CONSTRAINT CK_Capacity
		DECLARE @ConstraintName nvarchar(200)
		SELECT @ConstraintName = Name FROM SYS.DEFAULT_CONSTRAINTS WHERE PARENT_OBJECT_ID = OBJECT_ID('FLIGHTS') 
		AND PARENT_COLUMN_ID = (SELECT column_id FROM sys.columns WHERE NAME = N'booked_capacity' AND object_id = OBJECT_ID(N'FLIGHTS'))
		IF @ConstraintName IS NOT NULL
		EXEC('ALTER TABLE FLIGHTS DROP CONSTRAINT ' + @ConstraintName)
		IF EXISTS (SELECT * FROM syscolumns WHERE id=object_id('FLIGHTS') AND name='booked_capacity')
		EXEC('ALTER TABLE FLIGHTS DROP COLUMN booked_capacity')
	END

	ALTER TABLE FLIGHTS ADD booked_capacity int NOT NULL DEFAULT(0);
	
	ALTER TABLE FLIGHTS ADD CONSTRAINT CK_Capacity CHECK (booked_capacity <= capacity);
END
GO

--EXEC [sp_resetFlightCapacity];

ALTER TABLE FLIGHTS ADD booked_capacity int NOT NULL DEFAULT(0);
GO
ALTER TABLE FLIGHTS ADD CONSTRAINT CK_Capacity CHECK (booked_capacity <= capacity);
GO

CREATE TABLE USERS
(
username varchar(20) PRIMARY KEY,
password varchar(20),
balance float,
CONSTRAINT CK_User_Balance CHECK (balance >= 0)
)

CREATE TABLE RESERVATIONS
(
reservation_id int NOT NULL IDENTITY(1,1) PRIMARY KEY NONCLUSTERED,
username varchar(20),
date int,
month int,
year int,
paid int NOT NULL DEFAULT(0),
flight_id_1 int,
flight_id_2 int NULL,
total_price float,
cancelled int NOT NULL DEFAULT(0),
CONSTRAINT FK_Res_UserName FOREIGN KEY (username)     
    REFERENCES USERS (username),
CONSTRAINT FK_Res_FlightId1 FOREIGN KEY (flight_id_1)     
    REFERENCES FLIGHTS (fid),
CONSTRAINT FK_Res_FlightId2 FOREIGN KEY (flight_id_2)     
    REFERENCES FLIGHTS (fid),
CONSTRAINT CK_Paid CHECK (paid >= 0 AND paid <= 1),
CONSTRAINT CK_Cancelled CHECK (cancelled >= 0 AND cancelled <= 1),
CONSTRAINT CK_User_Per_Day UNIQUE (username, date), 
CONSTRAINT CK_Res_Id CHECK (reservation_id > 0),
)

CREATE NONCLUSTERED INDEX nc_idx_res_username_date ON RESERVATIONS (username, date);

CREATE CLUSTERED INDEX idx_res_username_resid ON RESERVATIONS (username, reservation_id);

/****** Object:  View [dbo].[view_Reservations]    Script Date: 12/3/2017 7:45:24 PM ******/
DROP VIEW IF EXISTS [dbo].[view_Reservations]
GO

/****** Object:  View [dbo].[view_Reservations]    Script Date: 12/3/2017 7:45:24 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


CREATE VIEW [dbo].[view_Reservations] AS
SELECT * FROM dbo.Reservations WHERE cancelled = 0

GO


/****** Object:  View [dbo].[[view_Reservations_detail]]    Script Date: 12/3/2017 7:45:24 PM ******/
DROP VIEW IF EXISTS [dbo].[view_Reservations_detail]
GO

/****** Object:  View [dbo].[[view_Reservations_detail]]    Script Date: 12/3/2017 7:45:24 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


GO

/****** Object:  StoredProcedure [dbo].[sp_view_Reservations_detail]    Script Date: 12/3/2017 8:12:16 PM ******/
DROP PROCEDURE IF EXISTS [dbo].[sp_view_Reservations_detail]
GO

/****** Object:  StoredProcedure [dbo].[sp_view_Reservations_detail]    Script Date: 12/3/2017 8:12:16 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [dbo].[sp_view_Reservations_detail]
	-- Add the parameters for the stored procedure here
	@username varchar(20)
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
	SELECT R.reservation_id as reservation_id, R.paid as paid, 
		R.date as date,
		R.month as month,
		R.year as year,
		R.total_price as total_price,
		A.fid as A_fid, 
		A.year as A_year,
		A.month_id as A_month_id,
		A.day_of_month A_day_of_month,
		A.day_of_week_id as A_day_of_week_id,
		A.carrier_id as A_carrier_id,
		A.flight_num as A_flight_num,
		A.origin_city as A_origin_city,
		A.dest_city as A_dest_city,
		A.actual_time as A_actual_time,
		A.distance as A_distance,
		A.capacity as A_capacity,
		A.price as A_price,
		B.fid as B_fid, 
		B.year as B_year,
		B.month_id as B_month_id,
		B.day_of_month B_day_of_month,
		B.day_of_week_id as B_day_of_week_id,
		B.carrier_id as B_carrier_id,
		B.flight_num as B_flight_num,
		B.origin_city as B_origin_city,
		B.dest_city as B_dest_city,
		B.actual_time as B_actual_time,
		B.distance as B_distance,
		B.capacity as B_capacity,
		B.price as B_price
		FROM view_Reservations R
		INNER JOIN Flights A ON R.flight_id_1 = A.fid
		LEFT OUTER JOIN Flights B ON R.flight_id_2 = B.fid
		WHERE R.username = @username
END

GO


/****** Object:  StoredProcedure [dbo].[sp_pay_reservation]    Script Date: 12/3/2017 8:12:16 PM ******/
DROP PROCEDURE IF EXISTS [dbo].sp_pay_reservation
GO

/****** Object:  StoredProcedure [dbo].[sp_pay_reservation]    Script Date: 12/3/2017 8:12:16 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [dbo].sp_pay_reservation
	-- Add the parameters for the stored procedure here
	@username varchar(20),
	@res_id int,
	@price float
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
	BEGIN TRANSACTION
	UPDATE Users SET balance=balance - @price WHERE username = @username;
	UPDATE Reservations SET paid=paid + 1 WHERE reservation_id = @res_id;
	COMMIT TRANSACTION
END

GO






/****** Object:  StoredProcedure [dbo].[sp_cancel_reservation]    Script Date: 12/3/2017 8:12:16 PM ******/
DROP PROCEDURE IF EXISTS [dbo].sp_cancel_reservation
GO

/****** Object:  StoredProcedure [dbo].[sp_cancel_reservation]    Script Date: 12/3/2017 8:12:16 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date,,>
-- Description:	<Description,,>
-- =============================================
CREATE PROCEDURE [dbo].sp_cancel_reservation
	-- Add the parameters for the stored procedure here
	@username varchar(20),
	@res_id int,
	@refund float
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
	BEGIN TRANSACTION
	UPDATE Flights SET booked_capacity = booked_capacity - 1 WHERE fid IN 
		(SELECT flight_id_1 FROM Reservations WHERE reservation_id = @res_id UNION SELECT flight_id_2 FROM Reservations WHERE reservation_id = @res_id AND flight_id_2 IS NOT NULL)
	UPDATE Users SET balance=balance + @refund WHERE username = @username;
	UPDATE Reservations SET cancelled=cancelled + 1 WHERE reservation_id = @res_id;
	COMMIT TRANSACTION
END
GO


