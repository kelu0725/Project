
-- step 1 create a database, use the same database
drop database if exists twitter_db;
create database twitter_db;
alter database twitter_db default character set utf8mb4 collate utf8mb4_bin;
use twitter_db;

-- Step 2 create q3_tweet table (2 columns)
drop table if exists `q3_tweet`;
create table `q3_tweet` (
    `tid` BIGINT NOT NULL,
    `time` BIGINT NOT NULL,
    `uid` BIGINT NOT NULL,
    `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  	`score` INT NOT NULL,
  	`freq` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    primary key (`tid`),
    INDEX uid_time (uid, time)
) ENGINE = MyISAM
CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Step 3 load data to phase_two table
load data local infile 'Q3_data_final.tsv' into table q3_tweet character set utf8mb4 columns terminated by '\t' ESCAPED BY '' LINES TERMINATED BY '\n';

-- CREATE INDEX uid_idx ON q3_tweet (uid);