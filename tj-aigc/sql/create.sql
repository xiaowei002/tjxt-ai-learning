use tj_aigc;
create table chat_memory
(
    id              bigint primary key,
    conversation_id varchar(255) not null unique,
    content         text
);