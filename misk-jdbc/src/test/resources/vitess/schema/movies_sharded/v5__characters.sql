CREATE TABLE `characters` (
    `id` bigint unsigned NOT NULL PRIMARY KEY,
    `created_at` timestamp(3) NOT NULL DEFAULT NOW(3),
    `updated_at` timestamp(3) NOT NULL DEFAULT NOW(3) ON UPDATE NOW(3),
    `name` varchar(200) NOT NULL,
    `movie_id` bigint NOT NULL,
    `actor_id` bigint NULL,
    INDEX `movie_id_idx` (`movie_id`),
    UNIQUE KEY `unq_movie_id_name_actor_id` (`movie_id`, `name`, `actor_id`)
) ENGINE=InnoDB;