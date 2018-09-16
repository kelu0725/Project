-- step 1 create a database
drop database if exists twitter_db;
create database twitter_db;
alter database twitter_db default character set utf8mb4 collate utf8mb4_bin;
use twitter_db;


-- Step 2 q2_tweet table (2 columns)
drop table if exists `q2_tweet`;
create table `q2_tweet` (
	`word` varchar(21) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
	`hashtags` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
	primary key (word)
) ENGINE = MyISAM
CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Step 3 load data to q2_tweet table (18 min 34.67 sec)
load data local infile 'q2_tweet_filtered.tsv' into table q2_tweet character set utf8mb4 columns terminated by '\t' LINES TERMINATED BY '\n';


-- Step 4 create q3_tweet table
drop table if exists `q3_tweet`;
create table `q3_tweet` (
    `tid` BIGINT NOT NULL,
    `time` BIGINT NOT NULL,
    `uid` BIGINT NOT NULL,
    `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  	`score` INT NOT NULL,
  	`freq` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    primary key (`tid`)
) ENGINE = MyISAM
CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Step 5 load data to q3_tweet table
load data local infile 'Q3_data_final.tsv' into table q3_tweet character set utf8mb4 columns terminated by '\t' ESCAPED BY '' LINES TERMINATED BY '\n';

-- CREATE INDEX uid_idx ON q3_tweet (uid);
create index uid_time on q3_tweet (uid, time);


-- Step 6 create q4_tweet table
drop table if exists `q4_tweet`;
create table `q4_tweet` (
    `tid` BIGINT NOT NULL,
    `timestamp` varchar(31) NOT NULL,
    `uid` BIGINT NOT NULL,
    `user_name` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `favorite_count` INT NOT NULL,
    `retweet_count` INT NOT NULL,
    `text` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    primary key (`tid`)
) ENGINE = InnoDB
CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Step 7 load data to q4_tweet table
load data local infile 'q4_tweet_sorted.tsv' into table q4_tweet character set utf8mb4 columns terminated by '\t' ESCAPED BY '' LINES TERMINATED BY '\n';

-- CREATE INDEX uid_idx ON q4_tweet (uid);
create index uid_idx on q4_tweet (uid);
