CREATE TABLE project_user_ref (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT,
  `user_name` VARCHAR(128),
  `project_id` INT
);

create unique index project_user_ref_id on project_user_ref (`id`);