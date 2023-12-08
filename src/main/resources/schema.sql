CREATE TABLE if not exists payment(
	id		BIGSERIAL PRIMARY KEY NOT NULL,
	bill_ref_Numer BIGSERIAL NOT NULL,
	user_id varchar(255) NOT NULL,
	bill_amount integer NOT NULL,
	payment_amount integer NOT NULL,
	status varchar(25),
	user_notified varchar(1),
	created_date timestamp NOT NULL,
	last_modified_date timestamp NOT NULL,
	version integer NOT NULL,
	created_by varchar(255) NOT NULL,
	last_modified_by varchar(255) NOT NULL

);