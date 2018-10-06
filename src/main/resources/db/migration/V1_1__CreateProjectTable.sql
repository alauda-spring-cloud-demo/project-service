CREATE TABLE projects (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `owner_id` INT,
  `owner_name` VARCHAR(128),
  `name` VARCHAR(128),
  `create_time` timestamp default current_timestamp
);

create unique index ix_project_id on projects (`id`);