<?php
$DB_FILE = __DIR__ . '/../recipes.sqlite';

$conn = new SQLite3($DB_FILE);


$conn->exec('CREATE TABLE IF NOT EXISTS recipes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    author TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    recipe TEXT NOT NULL
)');
