insert into "users" ("id", "name", "password", "created", "version")
values (0, 'admin', '$2a$10$7b.9iLgXFVh.r1u9HEbMv.EDL3JcJgldsWHUg4etSUh4wCNGuExye', now(), 0)
on conflict do nothing;
insert into "user_roles" ("id", "role")
values (0, 'admin')
on conflict do nothing;
insert into "user_roles" ("id", "role")
values (0, 'user')
on conflict do nothing;
insert into "users" ("id", "name", "password", "created", "version")
values (1, 'user', '$2a$10$7b.9iLgXFVh.r1u9HEbMv.EDL3JcJgldsWHUg4etSUh4wCNGuExye', now(), 0)
on conflict do nothing;
insert into "user_roles" ("id", "role")
values (1, 'user')
on conflict do nothing;
insert into "projects" ("id", "name", "user_id", "created", "version")
values (0, 'Work', 1, now(), 0)
on conflict do nothing;

alter sequence if exists hibernate_sequence restart with 10;
alter sequence if exists users_seq restart  with 10;
