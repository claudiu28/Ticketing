CREATE TABLE user
(
    id       integer primary key autoincrement,
    username text not null,
    password text not null
);

CREATE TABLE match
(
    id                    integer primary key autoincrement,
    team_a                text     not null,
    team_b                text     not null,
    price_ticket          real     not null,
    number_of_seats_total int      not null,
    match_type            text     not null,
    check (match_type in ('GROUPS',
                          'SIXTEENTHS',
                          'EIGHTEENTHS',
                          'QUARTERS',
                          'SEMIFINALS',
                          'FINALS'))
);

CREATE TABLE ticket(
                       id integer primary key autoincrement ,
                       match_id int not null,
                       first_name text not null,
                       last_name text not null,
                       address text not null,
                       number_of_seats_ticket integer not null,
                       foreign key (match_id) references match(id)
);

INSERT INTO user(username, password) values ('Marian', 'Vanzator1234');
INSERT INTO match(team_a, team_b, price_ticket, number_of_seats_total, match_type) VALUES ('Barca', 'Real Madrid', 50.99, 200, 'GROUPS');
INSERT INTO match(team_a, team_b, price_ticket, number_of_seats_total, match_type) VALUES ('Liverpool', 'Man Utd',  150.99, 400, 'GROUPS');
INSERT INTO match(team_a, team_b, price_ticket, number_of_seats_total, match_type) VALUES ('Barca', 'ATM Madrid', 250.99, 500, 'GROUPS');